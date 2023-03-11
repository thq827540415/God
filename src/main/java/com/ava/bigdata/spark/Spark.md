Hive on Spark：只有计算引擎使用Spark，SQL的解析、转换、优化等都是Hive完成的。

Spark on Hive：只有元数据使用了Hive，对SQL的解析、转换、优化等都是Spark完成的。 --> SparkSQL

bin/spark-submit --class org.apache.spark.examples.SparkPi --master yarn --deploy-mode client --conf spark.yarn.archive=hdfs://bigdata01:9000/libs/spark/__spark__.zip --conf spark.yarn.jars=hdfs://bigdata01:9000/libs/spark/jars/*.jar examples/jars/spark-examples_2.12-3.1.2.jar 10


## 一、基础概念

1. RDD的5大特性
   1. A list of partitions -> `protected def getPartitions: Array[Partition]`
   2. A function for computing each split -> `def compute(split: Partition, context: TaskContext): Iterator[T]`
   3. A list of dependencies on other RDDs -> `protected def getDependencies: Seq[Dependency[_]] = deps`
   4. Optionally, a Partitioner for key-value RDDs -> `@transient val partitioner: Option[Partitioner] = None`
   5. Optionally, a list of preferred locations to compute each split -> `protected def getPreferredLocations(split: Partition): Seq[String] = Nil`
2. Spark的Transformation算子用于构建DAG计算流图，底层使用迭代器对计算逻辑进行封装（延迟计算），当调用Action算子时，会从后往前调用迭代器的next方法，从而从数据源读取数据进行处理。
3. Spark的Driver中实现分布式调度最重要的三大组件DAGScheduler、TaskScheduler和SchedulerBackend
   1. DAGScheduler：以Action算子为起点，从后往前回溯DAG，以Shuffle操作为边界去划分Stages。
   2. 
4. 





## 二、性能调优

1. 优化角度
   1. SQL语句优化
   2. 数据倾斜处理
   3. 用户代码优化
   4. 配置参数优化 --> 大数据运维
   5. 开源软件代码优化

## 三、源码分析

#### 1. spark-submit流程分析（只讨论yarn-cluster，其他的类似）

1. 通过$SPARK_HOME/bin/spark-submit脚本调用org.apache.spark.deploy.SparkSubmit伴生对象中的main方法。

2. 然后通过doSubmit()将命令行参数解析成SparkSubmitArguments，然后进行submit，调用runMain方法。

   ```scala
   private[spark] class SparkSubmit extends Logging {
     private def runMain(args: SparkSubmitArguments, uninitLog: Boolean): Unit = {
       // 模式匹配，从SparkSubmitArguments中提取出四个重要的参数，重点关注childMainClass
       val (childArgs, childClasspath, sparkConf, childMainClass) = prepareSubmitEnvironment(args)
   
       var mainClass: Class[_] = null
   
       try {
         mainClass = Utils.classForName(childMainClass)
       } 
       
       // 通过比较类本身，而不是对象，判断mainClass是否继承/实现于SparkApplication
       val app: SparkApplication = if (classOf[SparkApplication].isAssignableFrom(mainClass)) {
         // 其中YarnClusterApplication extends SparkApplication
         mainClass.getConstructor().newInstance().asInstanceOf[SparkApplication]
       } else {
         new JavaMainApplication(mainClass)
       }
   
       try {
         app.start(childArgs.toArry, sparkConf)
       }
       ...
     }
     
     
     
     // 只贴出childMainClass的获取逻辑
     private[deploy] def prepareSubmitEnvironment(
       args: SparkSubmitArguments, 
       conf: Option[HadoopConfiguration] = None): (Seq[String], Seq[String], SparkConf, String) = {
       // Return values 
       val childArgs = new ArrayBuffer[String]()
       val childClasspath = new ArrayBuffer[String]()
       val sparkConf = args.toSparkConf()
       var childMainClass = ""
   
       // Set the cluster manager
       val clusterManager: Int = args.master match {
         case "yarn" => YARN
         case m if m.startsWith("local") => LOCAL
       }
   
       // Set the deploy mode
       var deployMode: Int = args.deployMode match {
         case "cluster" => CLUSTER
       }
   
       val isYarnCluster = clusterManager == YARN && deployMode == CLUSTER
   
       if (deployMode == CLIENT) {
         childMainClass = args.mainClass
       }
   
       if (isYarnCluster) {
         childMainClass = "org.apache.spark.deploy.yarn.YarnClusterApplication"
         // 将用户代码的入口封装在childArgs中
         childArgs += ("--class", args.mainClass)
       }
       ...
   
       (childArgs.toSeq, childClasspath.toSeq, sparkConf, childMainClass)
     }
   }
   ```

3. 由上伪代码可知，最终通过app.start()执行，其中app为YarnClusterApplication，具体代码如下

   ```scala
   private[spark] class YarnClusterApplication extends SparkApplication {
   
     override def start(args: Array[String], conf: SparkConf): Unit = {
       // SparkSubmit would use yarn cache to distribute files & jars in yarn mode,
       // so remove them from sparkConf here for yarn mode.
       conf.remove("spark.jars")
       conf.remove("spark.files")
   
       // org.apache.spark.deploy.yarn.Client
       new Client(new ClientArguments(args), conf).run()
   	}
   }
   ```

4. Client的run()如下，通过YarnClient向RM申请一个Container，启动org.apache.spark.deploy.yarn.ApplicationMaster

   ```scala
   privata[spark] class Client(
     val args: ClientArguments,
     val sparkConf: SparkConf) 
   extends Logging {
     
     private val yarnClient = YarnClient.createYarnClient
    	private val isClusterMode = sparkConf.get("spark.submit.deployMode", "client") == "cluster"
     
     def run(): Unit = {
       this.appId = submitApplication()
     }
     
     def submitApplication(): ApplicationId = {
       try {
         // Get a new appliation from our RM
         val newApp = yarnClient.createApplication()
         // RM返回集群的信息，此时并没有申请container
         val newAppResponse = newApp.getNewApplicationResponse
         appId = newAppResponse.getApplicationId
         
         // 设置运行AM的Container的上下文环境
         val containerContext = createContainerLaunchContext(newAppResponse)
         // 设置提交应用的上下文环境
         val appContext = createApplicationSubmissionContext(newApp, containerContext)
         
         // submit the application
         // 申请container，并运行ApplicationMaster中的main方法
         yarnClient.submitAppliation(appContext)
         
         appId
       }
     }
       
     |
     |
     V
     
     private def createContainerLaunchContext(newAppResponse: GetNewApplicationResponse): ContainerLaunchContext = {
       val amContainer = Records.newRecord(class[ContainerLaunchContext])
   
       val amClass = if (isClusterMode) {
         Utils.classForName("org.apache.spark.deploy.yarn.ApplicationMaster").getName
       } else {
         // 如果是client模式，则driver在本地执行
         Utils.classForName("org.apache.spark.deploy.yarn.ExecutorLauncher").getName
       }
       
   		val amArgs =
         Seq(amClass) ++ userClass ++ userJar ++ primaryPyFile ++ primaryRFile ++ userArgs ++
         Seq("--properties-file", buildPath(Environment.PWD.$$(), LOCALIZED_CONF_DIR, SPARK_CONF_FILE))
       
       // 用于启动AM的命令
       val commands = prefixEnv ++
         Seq(Environment.JAVA_HOME.$$() + "/bin/java", "-server") ++
         javaOpts ++ amArgs ++
         Seq(
           "1>", ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout",
           "2>", ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr")
       val printableCommands = commands.map(s => if (s == null) "null" else s).toList
       amContainer.setCommands(printableCommands.asJava)
       
       amContainer
   	}
       
     |
     |
     V
     
     def createApplicationSubmissionContext(
       newApp: YarnClientApplication, 
       containerContext: ContainerLaunchContext): ApplicationSubmissionContext = {
       val appContext = newApp.getApplicationSubmissionContext
       // 设置容器运行时上下文环境
      	appContext.setAMContainerSpec(containerContext)
       
       appContext
     }
   }
   ```

5. 向RM申请完container后，启动ApplicationMaster/Executor（根据上面amClass来决定）

   ```scala
   object ApplicationMaster extends Logging {
     private var master: Application = _
     
     def main(args: Array[String]): Unit = {
       val amArgs = new ApplicationMasterArguments(args)
       master = new ApplicationMaster(amArgs)
       System.exit(master.run())
     }
   }
   
   object ExecutorLauncher {
     def main(args: Array[String]): Unit = {
       ApplicationMaster.main(args)
     }
   }
   
   private[spark] class ApplicationMaster(args: ApplicationMasterArguments) extends Logging {
     private val isClusterMode = args.userClass != null
     @volatile private var userClassThread: Thread = _
     // In cluster mode, used to tell the AM when the user's SparkContext has been initialized.
     private val sparkContextPromise = Promise[SparkContext]()
     
   	final def run(): Int = {
       doAsUser {
         runImpl()
       }
       exitCode
     }
     
     |
     |
     V
     
     private def runImpl(): Unit = {
       try {
         if (isClusterMode) {
           runDriver()
         } else {
           runExecutorLauncher()
         }
       }
     }
     
     |
     |
     V
     
     private def runDriver(): Unit = {
       // 新开一个线程执行用户代码，也就是driver
       userClassThread = startUserApplication()
       
       try {
         // 超时等待结果
         val sc = ThreadUtils.awaitResult(sparkContextPromise.future, 
                                          Duration(totalWaitTime, TimeUnit.MILLISECONDS))
         // 当SparkContext初始化完成后，DAGScheduler、TaskScheduler和SchedulerBackend也初始化完成，开始建立通信
         if (sc != null) {
           val userConf = sc.getConf
           val host = userConf.get("spark.driver.host")
           val port = userConf.get("spark.driver.port").toInt
           // 向RM注册AM
           registerAM(host, port, userConf, sc.ui.map(_.webUrl))
           
           // 将与driver通信的引用封装，发送给Executor
           val driverRef = rpcEnv.setupEndpointRef(
             RpcAddress(host, port), 
             YarnSchedulerBackend.ENDPOINT_NAME)
           // 向RM申请执行Executor的container
           createAllocator(driverRef, userConf)
         }
         // 唤醒被sparkContextPromise阻塞的userClassThread
         resumeDriver()
         // 等待用户代码执行完毕，才继续往下执行
         userClassThread.join()
       } finally {
         resumeDriver()
       }
     }
   }
   ```

6. runDriver方法中的细节

   ```scala
   /**
    * 1. 在AM中新开一个线程执行用户代码 -> Driver
    */
   private def startUserApplication(): Thread = {
     var userArgs = args.userArgs
     val mainMethod = userClassLoader.loadClass(args.userClass)
     .getMethod("main", classOf[Array[String]])
   
     val userThread = new Thread {
       override def run(): Unit = {
         try {
           // 判断用户代码中的main方法是否是static修饰的
           if (!Modifier.isStatic(mainMethod.getModifiers)) {
           } else {
             // 执行到SparkContext时，其中的_taskScheduler.postStartHook()会阻塞userThread
             mainMethod.invoke(null, userArgs.toArray)
             finish(FinalApplicationStatus.SUCCEEDED, ApplicationMaster.EXIT_SUCCESS)
             logDebug("Done running user class")
           }
         } finally {
           sparkContextPromise.trySuccess(null)
         }
       }
     }
     userThread.setName("Driver")
     userThread.start()
     userThread
   }
   
   /**
    * 2. _taskScheduler.postStartHook()最终调用sparkContextInitialized进行阻塞userThread
    */
   private def sparkContextInitialized(sc: SparkContext) = {
     sparkContextPromise.synchronized {
       // Notify runDriver function that SparkContext is available
       sparkContextPromise.success(sc)
       // Pause the user class thread in order to make proper initialization in runDriver function.
       sparkContextPromise.wait()
     }
   }
   
   /**
    * 3. 申请执行Executor的Container
    */
   private def createAllocator(dirverRef: RpcEndpointRef, _sparkConf: SparkConf): Unit = {
     logInfo {
       val dummyRunner = new ExecutorRunnable(None, yarnCConf, _sparkConf, driverUrl, "<executorId>", 
                                              "<hostname>", executorMemory, execturoCores,  appId, securityMgr, localResources)
       dummyRunner.launchContextDebugInfo()
     }
   
     // allocator = YarnAllocator
     allocator = client.createAllocator(
       yarnConf,
       _sparkConf,
       driverUrl,
       driverRef,
       securityMgr,
       localResources)
   
     // 向RM申请Container启动
     allocator.allocateResources()
   }
   ```

7. 向RM申请资源启动Executor

   ```scala
   private[yarn] class YarnAllocator(...) extends Logging {
     // For testing
     private val launchContainers = sparkConf.getBoolean("spark.yarn.launchContainers", true)
     
     def allocateResources(): Unit = synchronized {
       // 使用AM向RM申请Container，Container只是一组运行在NM上的资源的抽象，并不是JVM进程，没有main方法
       val allocateResponse = amClient.allocate(progressIndicator)
       val allocatedContainers = allocateResponse.getAllocatedContainers
       
       if (allocatedContainers.size > 0) {
         handleAllocatedContainers(allocatedContainers.asScala)
       }
     }
     
     |
     |
     V
     
     def handleAllocatedContainers(allocatedContainers: Seq[Container]): Unit = {
       val containersToUse = new ArrayBuffer[Container](allocatedContainers.size)
       
       runAllocatedContainers(containersToUse)
     }
     
     |
     |
     V
     
     private def runAllocatedContainers(containersToUse: ArrayBuffer[Container]): Unit = {
       for (container <- containersToUse) {
   			if (runningExecutors.size() < targetNumExecutors) {
           numExecutorsStarting.incrementAndGet()
           if (launchContainers) {
             launcherPool.execute(new Runnable {
               override def run(): Unit = {
                 try {
                   new ExecutorRunnable(
                     Some(container),
                     conf,
                     sparkConf,
                     driverUrl,
                     executorId,
                     executorHostname,
                     executorMemory,
                     executorCores,
                     appAttemptId.getApplicationId.toString,
                     securityMgr,
                     localResources
                   ).run()
                   updateInternalState()
                 }
               }
             })
   				}
         }
       }
     }
   }
   ```

8. ExecutorRunnable中定义Executor的启动脚本等信息

   ```scala
   private[yarn] class ExecutorRunnable(container: Option[Container], ...) extends Logging {
     var nmClient: NMClient = _
     
     def run(): Unit = {
       // 初始化NodeManager客户端
       nmClient = NMClient.createNMClient()
       startContainer()
     }
     
     def startContainer(): java.util.Map[String, ByteBuffer] = {
   		val ctx = Records.newRecord(classOf[ContainerLaunchContext])
         .asInstanceOf[ContainerLaunchContext]
       val commands = prepareCommand()
       
       ctx.setCommands(commands.asJava)
       
       try {
         // 接下来的事就交给Yarn了吧
         nmClient.startContainer(container.get, ctx)
       }
     }
     
     private def prepareCommand(): List[String] = {
   		val commands = prefixEnv ++
         Seq(Environment.JAVA_HOME.$$() + "/bin/java", "-server") ++
         javaOpts ++
         Seq("org.apache.spark.executor.CoarseGrainedExecutorBackend",
           "--driver-url", masterAddress,
           "--executor-id", executorId,
           "--hostname", hostname,
           "--cores", executorCores.toString,
           "--app-id", appId) ++
         userClassPath ++
         Seq(
           s"1>${ApplicationConstants.LOG_DIR_EXPANSION_VAR}/stdout",
           s"2>${ApplicationConstants.LOG_DIR_EXPANSION_VAR}/stderr")
       
       commands.map(s => if (s == null) "null" else s).toList
     }
   }
   ```

9. 发送到

