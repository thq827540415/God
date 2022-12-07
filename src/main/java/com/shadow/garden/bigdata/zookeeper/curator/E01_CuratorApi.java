package com.shadow.garden.bigdata.zookeeper.curator;

import com.solitude.util.BasicEnvUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.CloseableUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
public class E01_CuratorApi {
    private static final CuratorFramework client = BasicEnvUtils.getCuratorInstance();

    static {
        client.start();
    }

    /**
     * create path [data]
     */
    private static void createNode() {
        try {
            // 当节点存在时，不会抛异常
            client.create()
                    // 父节点不存在时，先创建父节点，且父节点为永久节点
                    .creatingParentsIfNeeded()
                    // 创建永久节点
                    // 临时节点下不能创建子节点
                    .withMode(CreateMode.PERSISTENT)
                    // 异步创建节点
                    // 在ZK中所有异步通知事件都是由main-EventThread串行处理，碰上复杂的处理逻辑，可以传入一个Executor实例
                    .inBackground((client, event) ->
                            log.info("create callback successful and eventType is: {}", event.getType()))
                    // Path must start with / character
                    .forPath("/test", "test".getBytes(StandardCharsets.UTF_8));
            // 让异步线程能够存活
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseableUtils.closeQuietly(client);
        }
    }

    /**
     * 读取节点的关键关键方法有3个：
     * 1. checkExists()
     * 2. getData() -> get path
     * 3. getChildren()
     */
    private static void readNode() {
        String visitPath = "/test";
        try {
            Stat stat = client.checkExists().forPath(visitPath);
            if (Objects.nonNull(stat)) {
                log.info("Node exists!");
                log.info("get data is: {}", new String(
                        // 获取数据
                        client.getData()
                                // 获取该节点的State
                                .storingStatIn(stat)
                                .forPath(visitPath)));
            } else {
                log.info("Node doesn't exists!");
            }
            log.info("children has {}, ",
                    // 获取子节点
                    client.getChildren()
                            .forPath("/"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseableUtils.closeQuietly(client);
        }
    }

    /**
     * set path data
     */
    private static void setNode() {
        try {
            client.setData()
                    // withVersion用来实现CAS
                    // .withVersion(-1)
                    .inBackground((client, event) ->
                            log.info("set callback successful and event type is: {}", event.getType()))
                    .forPath("/test", "hello".getBytes(StandardCharsets.UTF_8));
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseableUtils.closeQuietly(client);
        }
    }

    /**
     * delete path / deleteall path
     */
    private static void deleteNode() {
        try {
            // 若节点不存在，删除不会抛异常
            client.delete()
                    // 强制保证删除，只要会话有效，会在后台持续进行删除操作
                    .guaranteed()
                    // 删除节点对应的子节点
                    .deletingChildrenIfNeeded()
                    .withVersion(-1)
                    .inBackground((client, event) ->
                            log.info("delete callback successful and event type is: {}", event.getType()))
                    .forPath("/test");
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseableUtils.closeQuietly(client);
        }
    }

    public static void main(String[] args) {
        createNode();
    }
}
