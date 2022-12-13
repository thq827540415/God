### 一、YARN源码解析

YARN中的ApplicationMaster只是一个规范，MR中的规范为MRAppMaster。

1. 

### 二、MapReduce源码解析（ MR on YARN）

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

      4. 利用ResourceMgrDelegate生成YarnClientImpl对象 -> 与YARN有关的，详细见`YARN源码解析`：

         ```java
         public class ResourceMgrDelegate extends YarnClient {
             protected YarnClient client;
             
             public ResourceMgrDelegate(YarnConfiguration conf) {
                 ...
                 this.client = YarnClient.createYarnClient();
                 // 调用AbstractService（重要）中的init方法
                 init(conf);
                 // 调用AbstractService（重要）中的start方法
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
         - `status = submitter.submitJobInternal(Job.this, cluster)`.
         - 此方法内部才是MR的核心部分，定了很多的细节操作，具体见`MapReduce核心流程`。
      2. YARNRunner提交：
         - `status = submitClient.submitJob(jobId, submitJobDir.toString(), job.getCredentials())`.
         - 其中的submitClient为cluster.getClient() -> YARNRunner对象
      3. ResourceMgrDelegate提交：
         - `ApplicationId applicationId = resMgrDelegate.submitApplication(appContext)`.
         - 调用此方法前，会获取创建RM AM的一些必要信息，同时在appContext中封装好ApplicationId等信息：`createApplicationSubmissionContext(conf, jobSubmitDir, ts)`.
      4. YarnClientImpl提交：
         - `return client.submitApplication(appContext)`.
         - 其中的client为YarnClientImpl，同时返回该Job的ApplicationId（在submitJobInternal方法中就已经申请了ApplicationId了）。
      5. ResourceManager的代理对象rmClient提交：
         - `rmClient.submitApplication(request)`.
         - 不返回任何东西，最终通过ResourceManager的ClientRMService来进行submitApplication()的RPC服务处理。
      6. 向ResourceManager申请Container启动ApplicationMaster：
         1. 调用MRAppMaster.main()启动ApplicationMaster。
         2. ApplicationMaster向ResourceManager申请Container启动MapTask和ReduceTask。
         3. MapTask启动的入口：YarnChild.main() -> taskFinal.run(job, umbilical);其中taskFinal的实现类为MapTask。
         4. ReduceTask启动的入口：YarnChild.main()。

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
           
           // 创建工作目录，同时上传libjars、archives和jobJar等
           copyAndConfigureFiles(job, submitJobDir);
           
         	// 对Job的数据文件进行切片，返回逻辑切片个数，也就是MapTask个数，详见`MapReduce逻辑切片`
           // 生成切片信息，和切片元数据信息
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
   /**
    * 调用TextInputFormat的父类FileInputFormat中的getSplits方法
    */
   public abstract class FileInputFormat<K, V> extends InputForamt<K, V> {
       /**
        * MapTask读取的基本单位是InputSplit？其中默认为FileSplit
        * 其中定义了读取的范围[start, start + length]，和start所在的块的主机
        * 切片的具体操作见`MapTask执行源码详解`
        */
       public List<InputSplit> getSplits(JobContext job) throws IOException {
           // 第一个参数为1
           // 第二个参数默认为1，通过mapreduce.input.fileinputformat.split.minsize调整
           long minSize = Math.max(getFormatMinSplitSize(), getMinSplitSize(job));
           // 默认为Long.MAX_VALUE
           // 通过mapreduce.input.fileinputformat.split.maxsize调整
           long maxSize = getMaxSplitSize(job);
           
           // 其中InputSplit的默认实现为FileSplit，存储逻辑切片的结果
           List<InputSplit> splits = new ArrayList<InputSplit>();
           // 获取输入路径下所有文件及文件夹的状态，默认不递归去遍历路径下的文件夹
           // 底层使用fs.globStatus()获取文件状态
           List<FileStatus> files = listStatus(job);
           // FileStatus默认实现为LocatedFileStatus
           for (FileStatus file : files) {
               Path path = file.getPath();
               long length = file.getLen();
               if (length != 0) {
                   // 获取文件每个数据块的位置
                   BlockLocation[] blkLocations;
                   if (file instanceof LocatedFileStatus) {
                       blkLocations = ((LocatedFileStatus) file).getBlockLocations();
                   } else {
                       ...
                   }
                   // org.apache.hadoop.mapreduce.lib.input.TextInputFormat#isSplitable
                   // 可切分是逻辑上的，表示文件的每个块是否可以独立存在（例如：每个块内容不需要元数据即可读取）
                   if (isSplitable(job, path)) {
                       // 获取文件块大小，默认128MB
                       long blockSize = file.getBlockSize();
                       // 计算每个切片的大小，默认为blockSize
                       long splitSize = computeSplitSize(blockSize, minSize, maxSize);
                       
                       long bytesRemaining = length;
                       // SPLIT_SLOP为1.1
                       // 如果剩余数据的大小 > (splitSize * 1.1)，则进行逻辑切片
                       while (((double) bytesRemaining) / splitSize > SPLIT_SLOP) {
                           // 获取该块所在的位置
                           int blkIndex = getBlockIndex(blkLocations, length - bytesRemaining);
                           splits.add(makeSplit(path, length - bytesRemaining, splitSize,
                                               blkLocations[blkIndex].getHosts(),
                                               blkLocations[blkIndex].getCachedHosts()));
                           bytesRemaining -= splitSize;
                       }
                       
                       // 此时剩余数据的数据量∈[0, (splitSize * 1.1)]，单独形成一个逻辑分片
                       if (bytesRemaining != 0) {
                           int blkIndex = getBlockIndex(blkLocations, length - bytesRemaining);
                           splits.add(makeSplit(path, length - bytesRemaining, bytesRemaining,
                                               blkLocations[blkIndex].getHosts(),
                                               blkLocations[blkIndex].getCachedHosts()));
                       }
                   } else {
                       // 不可切分时，只生成一个切片，将该文件使用一个MapTask处理，可能会造成OOM
                       splits.add(makeSplit(path, 0, length, blkLocations[0].getHosts(),
                                           blkLocations[0].getCachedHosts()));
                   }
               } else {
                   // 如果文件为空，则创建一个空切片
                   splits.add(makeSplit(path, 0, length, new String[0]));
               }
           }
           return splits;
       }
       
       protected long computeSplitSize(long blockSize, long minSize, long maxSize) {
           return Math.max(minSize, Math.min(maxSize, blockSize));
       }
       ...
   }
   ```

4. **MapTask执行源码详解（org.apache.hadoop.mapred.MapTask#run）**

   1. 

5. **MapReduce的Shuffle详解**

   1. MapTask的数据写入到ReduceTask的流程即为Shuffle。
   2. 

6. **ReduceTask执行源码详解（org.apache.hadoop.mapred.ReduceTask#run）**

### 三、HDFS源码解析（待定。。。）
