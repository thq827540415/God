import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 使用开源框架Curator，专门用于解决分布式锁的框架，解决了原生JavaAPI开发的问题
 */
@Slf4j
public class CuratorAPI {

    private CuratorFramework client;

    /**
     * zk连接地址
     */
    private final String connStr = "localhost:2181,localhost:2182,localhost:2183";

    /**
     * 会话超时时间
     */
    private final int sessionTimeoutMs = 1000;

    /**
     * 连接超时时间
     */
    private final int connTimeoutMs = 1000;

    /**
     * 重试策略：第一次重试等待1s，第二次重试等待2s，第三次重试等待4s
     *         baseSleepTimeMs：等待时间的基本单位
     *         maxReties：最大重试次数
     */
    private final ExponentialBackoffRetry retryPolicy= new ExponentialBackoffRetry(1000, 3);

    /**
     * 工厂类中的静态方法newClient()
     */
    @Test
    public void createSimple() {
        client = CuratorFrameworkFactory.newClient(connStr, sessionTimeoutMs, connTimeoutMs, retryPolicy);
        log.info("{} client get successful!", client);
    }

    /**
     * 工厂类中的静态方法builder()
     */
    @Before
    public void createWithOpts() {
        client = CuratorFrameworkFactory.builder()
                .connectString(connStr)
                .sessionTimeoutMs(sessionTimeoutMs)
                .connectionTimeoutMs(connTimeoutMs)
                .retryPolicy(retryPolicy)
                .build();
        log.info("{} client get successful!", client);
    }

    /**
     * create path [data]
     */
    @Test
    public void createNode() {
        client.start();
        // 节点存在，不会抛异常
        try {
            client.create()
                    // 父节点不存在时，先创建父节点
                    .creatingParentsIfNeeded()
                    // 创建永久节点
                    .withMode(CreateMode.PERSISTENT)
                    // 异步创建节点
                    .inBackground((client, event) -> log.info("callback successful and event type is: {}", event.getType()))
                    .forPath("/test", "test".getBytes(StandardCharsets.UTF_8));
            TimeUnit.SECONDS.sleep(1);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseableUtils.closeQuietly(client);
        }
    }


    /**
     * 读取节点的关键方法主要有3个：（1）checkExists()判断节点是否存在
     *                        （2）getData()获取节点的数据
     *                        （3）getChildren()获取子节点列表
     *                        三者的调用都是去获取Builder实例 -> ExistsBuilder
     */
    @Test
    public void readNode() {
        client.start();
        try {
            // 判断节点是否存在
            Stat stat = client.checkExists().forPath("/test");
            if(Objects.nonNull(stat)) {
                log.info("Node already exists!");
                // 获取节点数据
                String data = new String(client.getData().forPath("/test"));
                log.info("get data is: {}", data);
            } else {
                log.error("Node not exists!");
            }
            // 获取子节点
            client.getChildren().forPath("/")
                    .forEach(children -> log.info("children has {}", children));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseableUtils.closeQuietly(client);
        }
    }

    /**
     * set path data
     */
    @Test
    public void updateNode() {
        client.start();
        try {
            client.setData()
                    .inBackground((client, event) -> log.info("callback successful and event type is: {}", event.getType()))
                    .forPath("/test", "hello".getBytes(StandardCharsets.UTF_8));
            TimeUnit.SECONDS.sleep(1);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseableUtils.closeQuietly(client);
        }
    }

    /**
     * delete [-v version] path
     */
    @Test
    public void deleteNode() {
        client.start();
        try {
            // 若节点不存在，删除不会抛异常
            client.delete()
                    // 删除节点对应的子节点
                    .deletingChildrenIfNeeded()
                    .withVersion(-1)
                    // 异步删除
                    .inBackground((client, event) -> log.info("callback successful and event type is: {}", event.getType()))
                    .forPath("/test");
            TimeUnit.SECONDS.sleep(1);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseableUtils.closeQuietly(client);
        }
    }
}
