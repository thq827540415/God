### 一、MapReduce源码解析

1. 提交MR任务，连接YARN的connect()：

   ```java
   private synchronized void connect()
             throws IOException, InterruptedException, ClassNotFoundException {
       if (cluster == null) {
         cluster = 
           ugi.doAs(new PrivilegedExceptionAction<Cluster>() {
                      public Cluster run()
                             throws IOException, InterruptedException, 
                                    ClassNotFoundException {
                        return new Cluster(getConfiguration());
                      }
           });
       }
   }
   ```

2. Job提交过程：

   1. 通过JobSubmitter：`JobSubmitter.submitJobInternal(Job.this, cluster);`
   2. 通过YARNRunner：`submitClient.submitJob(jobId, submitJobDir.toString(), job.getCredentials());`
   3. 通过ResourceMgrDelegate：`resMgrDelegate.submitApplication(appContext);`
   4. 通过YarnClientImpl：`client.submitApplication(appContext);`
   5. 通过ResourceManager的代理对象 -> ApplicationClientProtocol：`rmClient.submitApplication(request);`

   

   

