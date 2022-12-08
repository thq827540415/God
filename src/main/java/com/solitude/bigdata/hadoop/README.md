### 一、MapReduce源码解析（ MR on YARN）

1. **Job提交流程（org.apache.hadoop.mapreduce.Job#submit）**：

   ```java
   public class Job extends JobContextImpl implements JobContext {
       public void submit() throws IOException, InterruptedException, ClassNotFoundException {
           // 1. 确保Job的状态为DEFINE
           ensureState(JobState.DEFINE);
           // 2. 设置使用新的MRAPI
           setUseNewAPI();
           // 3. 初始化YARN连接 -> 初始化Cluster对象
           connect();
           // 获取提交器
           final JobSubmitter submitter = 
               getJobSubmitter(cluster.getFileSystem(), cluster.getClient());
           // 4. 提交Job
           status = ugi.doAs((PrivilegedExceptionAction) () -> {
               return submitter.submitJobInternal(Job.this, cluster);
           });
           state = JobState.RUNNING;
       }
       ...
   }
   ```

   1. connect方法详解：

      1. 通过connect方法， 初始化Job中的成员变量cluster：

         ```java
         public class Job extends JobContextImpl implements JobContext {
             private Cluster cluster;
         
             private synchronized void connect()
                   throws IOException, InterruptedException, ClassNotFoundException {
                 if (cluster == null) {
                   cluster = 
                     ugi.doAs((PrivilegedExceptionAction) () -> {
                         return new Cluster(getConfiguration());
                     });
                 }
             }
             ...
         }
         ```

      2. 通过Cluster的构造方法，调用initialize获取YARNRunner对象，伪代码如下：

         ```java
         public class Cluster {
             /**
              * ClientProtocol为YARNRunner
              */
             private ClientProtocol client;
         
             private void initialize(InetSocketAddress jobTrackAddr, Configuration conf) throws IOException {
                 synchronized(frameworkLoader) {
                     for (ClientProtocolProvider provider : frameworkLoader) {
                         ClientProtocol clientProtocol = null;
                         if (jobTrackAddr == null) {
                             // provider实质上是YarnClientProtocolProvider
                             clientProtocol = provider.create(conf);
                         }
                         if (clientProtocol != null) {
                             client = clientProtocol;
                         }
                         ...
                     }
                 }
              }
             ...
         }
         
         /**
          * 调用provider.create()获取YARNRunner对象
          */
         public class YarnClientProtocolProvider extends ClientProtocolProvider {
         
             @Override
             public ClientProtocol create(Configuration conf) throws IOException {
                 // if ("yarn".equals("mapreduce.framework.name"))
                 if (MRConfig.YARN_FRAMEWORK_NAME.equals(conf.get(MRConfig.FRAMEWORK_NAME))) {
                     return new YARNRunner(conf);
                 }
                 return null;
             }
             ...
         }
         ```

      3. 在YARNRunner构造方法中，生成ResourceMgrDelegate对象：

         ```java
         public class YARNRunner implements ClientProtocol {
             /**
              * 用于从MR到YARN的中间过度类，YARNRunner属于MR，YarnClient属于YARN
              */
             private ResourceMgrDelegate resMgrDelegate;
             
             public YARNRunner(Configuraion conf) {
                 this(conf, new ResourceMgrDelegate(new YarnConfiguration(conf)));
             }
             ...
         }
         ```

      4. 利用ResourceMgrDelegate生成YarnClientImpl对象 -> 与YARN有关的，详细见YARN源码解析：

         ```java
         public class ResourceMgrDelegate extends YarnClient {
             protected YarnClient client;
             
             public ResourceMgrDelegate(YarnConfiguration conf) {
                 ...
                 this.client = YarnClient.createYarnClient();
                 // 调用AbstractService中的init方法
                 init(conf);
                 // 调用AbstractService中的start方法
                 start();
             }
             ...
         }
         
         public class YarnClient extends AbstractService {
             public static YarnClient createYarnClient() {
                 YarnClient client = new YarnClientImpl();
                 return client;
             }
             ...
         }
         ```

      5. 构建YarnClientImpl对象，进而能初始化RM的代理对象rmClient：

         ```java
         public class YarnClientImpl extends YarnClient {
             /**
              * 实质上是ClientRMService
              */
             protected ApplicationClientProtocol rmClient;
             
             /**
              * 在ResourceMgrDelegate构造方法中调用的start方法，最终会调用该方法初始化rmClient
              */
             @Override
             protected void serviceStart() throws Exception {
                 rmClient = ClientRMProxy.createRMProxy(getConfig(), ApplicationClientProtocol.class);
                 ...
             }
         }
         ```

   2. 获取到YARN的连接对象cluster后，提交Job全过程：
      1. JobSubmitter提交：
         1. `status = submitter.submitJobInternal(Job.this, cluster)`.
         2. 此方法内部才是MR的核心部分，定了很多的细节操作，具体见`MapReduce核心流程`。
      2. YARNRunner提交：
         1. `status = submitClient.submitJob(jobId, submitJobDir.toString(), job.getCredentials())`.
         2. 其中的submitClient为cluster.getClient() -> YARNRunner对象
      3. ResourceMgrDelegate提交：
         1. `ApplicationId applicationId = resMgrDelegate.submitApplication(appContext)`.
         2. 调用此方法前，会获取创建RM AM的一些必要信息，同时在appContext中封装好ApplicationId等信息：`createApplicationSubmissionContext(conf, jobSubmitDir, ts)`.
      4. YarnClientImpl提交：
         1. `return client.submitApplication(appContext)`.
         2. 其中的client为YarnClientImpl，同时返回该Job的ApplicationId（在submitJobInternal方法中就已经申请了ApplicationId了）。
      5. ResourceManager的代理对象rmClient提交：
         1. `rmClient.submitApplication(request)`.
         2. 不返回任何东西，最终通过ResourceManager的ClientRMService来进行submitApplication()的RPC服务处理。

2. **MapReduce核心流程（org.apache.hadoop.mapreduce.JobSubmitter#submitJobInternal）**：

   ```java
   class JobSubmitter {
       /**
        * YARNRunner对象
        */
       private ClientProtocol submitClient;
       
       JobStatus submitJobInternal(Job job, Cluster cluster) {
           // 检查输出路径是否存在
           checkSpecs(job);
           
           // 添加该程序运行时各种组件的信息到DistributedCache中，程序在执行时,
           // 节点都会自动的把DistributedCache里面缓存的各种东西（小数据文件，配置等）同步到本地
           addMRFrameworkToDistributedCache(conf);
           
           // 获取Job执行时相关资源的存放根路径
           // 默认值为：/tmp/hadoop-yarn/staging/user/.staging
           Path jobStagingArea = JobSubmissionFiles.getStagingDir(cluster, conf);
           
           // 通过YARNRunner不断调用，最终通过rmClient向RM申请AM，同时返回ApplicationId，再封装成JobID返回
           JobID jobId = submitClient.getNewJobID();
           
           // Job对应的资源存放的完整路径
           // 默认值为：/tmp/hadoop-yarn/staging/user/.staging/jobId
           Path submitJobDir = new Path(jobStagingArea, jobId.toString());
           
           // todo jar文件，配置文件的上传？
           copyAndConfigureFiles(job, submitJobDir);
           
         	// 对Job的数据文件进行切片，返回逻辑切片个数，也就是MapTask个数，详见`数据切块`
           int maps = writeSplits(job, submitJobDir);
           
           // 生成job.xml的完整路径名
           Path submitJobFile = JobSubmissionFiles.getJobConfPath(submitJobDir);
           
           // 根据路径名，将job.xml文件上传到指定目录
           writeConf(conf, submitJobFile);
           
           // 最终通过YARNRunner提交Job，并返回Job的状态
           status = submitClient.submitJob(
             jobId, submitJobDir.toString(), job.getCredentials());
           ...
       }
   }
   ```

3. **MapReduce逻辑切片（org.apache.hadoop.mapreduce.lib.input.FileInputFormat#getSplits）**：

   ```java
   public abstract class FileInputFormat<K, V> extends InputForamt<K, V> {
       public List<InputSplit> getSplits(JobContext job) throws IOException {
           long minSize = Math.max(getFormatMinSplitSize(), getMinSplitSize(job));
           long maxSize = getMaxSplitSize(job);
           
           
       }
       ...
   }
   ```

   

### 二、YARN源码解析

1. 
