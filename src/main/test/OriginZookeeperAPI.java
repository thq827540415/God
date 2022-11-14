import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 原生API存在的问题：（1）会话连接是异步的，需要自己去处理，比如使用CountDownLatch
 *                 （2）Watch需要重复注册，不然就不能生效
 *                 （3）开发的复杂性高
 *                 （4）不支持多节点删除和创建，需要自己去递归
 */
@Slf4j
public class OriginZookeeperAPI {
    private ZooKeeper zkCli;
    private final CountDownLatch cdl = new CountDownLatch(1);

    @Before
    public void init() {
        try {
            zkCli = new ZooKeeper("localhost:2181,localhost:2182,localhost:2183", 2000, watchedEvent -> {
               if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                   log.info("linked successful!");
                   cdl.countDown();
               }
           });
            cdl.await();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * ls
     */
    @Test
    public void lsNode() {
        try {
            List<String> children = zkCli.getChildren("/", watchedEvent -> {
                log.info("children has changed!");
            });
            children.forEach(System.out::println);
            Thread.sleep(Long.MAX_VALUE);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * create
     */
    @Test
    public void createNode() {
        try {
            zkCli.create("/test", "test".getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * delete：不支持多节点删除和创建，需要自己去递归实现；exists + delete实现
     */
    public void deleteNode() {
    }

    @After
    public void close() {
        try {
            zkCli.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
