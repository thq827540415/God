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
             ...
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
             ... 
             private void initialize(InetSocketAddress jobTrackAddr, Configuration conf) throws IOException {
                 synchronized(frameworkLoader) {
                     for (ClientProtocolProvider provider : frameworkLoader) {
                         ClientProtocol clientProtocol = null;
                         if (jobTrackAddr == null) {
                             // provider实质上是YarnClientProtocolProvider
                             clientProtocol = provider.create(conf);
                         }
                 		...
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
          * 调用provider.create()获取YARNRunner对象。
          */
         public class YarnClientProtocolProvider extends ClientProtocolProvider {
             ...
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

      3. 利用YARNRunner生成ResourceMgrDelegate：

         ```java
         public class YARNRunner implements ClientProtocol {
             /**
              * 用于从MR到Yarn的中间过度
              */
             private ResourceMgrDelegate resMgrDelegate;
             ...
             public YARNRunner(Configuraion conf) {
                 this(conf, new ResourceMgrDelegate(new YarnConfiguration(conf)));
             }
             ...
         }
         ```

         

      4. 放电时

      

   2. 放电时

      ```java
      
      1. Job内部有一个Cluster cluster成员变量
      2. Cluster内部有一个YARNRunner client的成员变量 -> 通过YarnClientProtocolProvider创建。
      3. YARNRunner内部有一个ResourceMgrDelegate resMgrDelegate成员变量 -> YARNRunner构造器创建。
      4. ResourceMgrDelegate内部有一个YarnClientImpl client成员变量，。 -> YarnClient.createYarnClient()创建。
      5. YarnClientImpl内部有一个ApplicationClientProtocol rmClient的成员变量。 -> serviceStart()方法创建。
      ```

   3. Job提交过程 -> submitJobInternal()：
      1. 通过JobSubmitter：`JobSubmitter.submitJobInternal(Job.this, cluster)`，此方法内部才是MR的核心部分，定了很多的细节操作。
      2. 通过YARNRunner：`submitClient.submitJob(jobId, submitJobDir.toString(), job.getCredentials());
      3. 通过ResourceMgrDelegate：`resMgrDelegate.submitApplication(appContext)`
      4. 通过YarnClientImpl：`client.submitApplication(appContext)`
      5. 通过ResourceManager的代理对象 -> ApplicationClientProtocolPBClientImpl：`rmClient.submitApplication(request)`
      6. 最终通过ResourceManager组件中的ClientRMService来执行submitApplication()的RPC服务处理。

2. **MapReduce核心流程（org.apache.hadoop.mapreduce.JobSubmitter#submitJobInternal）**：

   1. checkSpecs(job)：检查输出路径是否存在。
   2. addMRFrameworkToDistributedCache(conf)：添加该程序运行时各种组件的信息到DistributedCache中，程序在执行时，节点都会自动的把DistributedCache里面缓存的各种东西（小数据文件，配置等）同步到本地。
   3. Path jobStagingArea = JobSubmissionFiles.getStagingDir(cluster, conf)：获取作业执行时相关资源的存放路径，默认：/tmp/hadoop-yarn/staing/提交作业用户名/.staging/，jobStaingArea + jobId为submitJobDir，其中包含了xxx.jar、job.xml和shell启动命令（用于RPC启动进程）。
   4. 记录提交作业的主机IP、主机名，并且设置配置信息。
   5. JobID jobId = submitClient.getNewJobID()：通过YARNRunner获取JobId，同时生成ApplicationId。
   6. copyAndConfigureFiles(job, submitJobDir)：jar文件，配置文件的上传。
   7. int maps = writeSplits(job, submitJobDir)：写分片数据文件job.splits和分片元数据文件job.splitmetainfo，计算MapTask任务数，最终调用TextInputFormat#getSplits。
   8. writeConf(conf, submitJobFile)：生成job.xml到submitJobDir。



### 二、YARN源码解析

1. 
