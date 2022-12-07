package com.shadow.garden.bigdata.zookeeper.curator;

import com.solitude.util.BasicEnvUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicInteger;
import org.apache.curator.framework.recipes.barriers.DistributedBarrier;
import org.apache.curator.framework.recipes.barriers.DistributedDoubleBarrier;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.RetryNTimes;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 应用场景
 */
@Slf4j
public class E02_Scenarios {
    private static final CuratorFramework client = BasicEnvUtils.getCuratorInstance();

    static {
        client.start();
    }

    public static void main(String[] args) {
        eventWatcher();
    }

    /**
     * 事件监听
     * Curator引入了Cache来实现对ZK服务端事件的监听，分为两类监听类型：节点监听和子节点监听
     */
    private static void eventWatcher() {
        String path = "/test";
        // 使用NodeCache监听指定ZK数据节点本身的变化
        try {
            client.create().forPath(path, "test".getBytes(StandardCharsets.UTF_8));

            NodeCache nodeCache = new NodeCache(client, path, false);
            nodeCache.start();

            // 设置监听
            nodeCache.getListenable()
                    .addListener(
                            () -> log.info("Node data update, new data: {}",
                                    new String(nodeCache.getCurrentData().getData())));

            client.setData().forPath(path, "hello".getBytes(StandardCharsets.UTF_8));
            TimeUnit.SECONDS.sleep(1);


            // 使用PathChildrenCache监听ZK数据节点的子节点变化情况
            PathChildrenCache pathChildrenCache = new PathChildrenCache(client, path, true);
            pathChildrenCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);

            pathChildrenCache.getListenable()
                    .addListener((curatorFramework, event) -> {
                        switch (event.getType()) {
                            case CHILD_ADDED:
                                log.info("CHILD_ADDED, {}", event.getData().getPath());
                                break;
                            case CHILD_UPDATED:
                                // 子节点数据变更
                                log.info("CHILD_UPDATED, {}", event.getData().getPath());
                                break;
                            case CHILD_REMOVED:
                                log.info("CHILD_REMOVED, {}", event.getData().getPath());
                                break;
                            default:
                                break;
                        }
                    });

            client.create()
                    .forPath(path + "/c1");
            TimeUnit.SECONDS.sleep(1);
            client.delete().deletingChildrenIfNeeded().forPath(path);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 分布式Master选举，原理同分布式锁一致
     * 多台机器同时向该节点创建一个子节点
     */
    private static void masterSelect() {
        try {
            for (int i = 0; i < 3; i++) {
                new Thread(() -> {
                    LeaderSelector selector = new LeaderSelector(
                            client,
                            // leader_path为临时节点
                            "/leader_path",
                            // Curator会在成功选举Master的时候，回调该监听器
                            new LeaderSelectorListenerAdapter() {
                                /**
                                 * 一旦执行完takeLeadership方法，Curator会立即释放Master，然后重启开始新一轮Master选举
                                 */
                                @Override
                                public void takeLeadership(CuratorFramework curatorFramework) throws Exception {
                                    log.info("成为Master角色");
                                    TimeUnit.SECONDS.sleep(3);
                                    log.info("完成Master的操作，释放Master");
                                }
                            });
                    // 自动重新放入队列中，参与Master的选举
                    selector.autoRequeue();
                    selector.start();
                }).start();
            }
            TimeUnit.SECONDS.sleep(Integer.MAX_VALUE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 分布式锁
     */
    private static void distributeLock() {
        final InterProcessLock lock = new InterProcessMutex(client, "/lock_path");
        final CountDownLatch cdl = new CountDownLatch(1);
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                try {
                    cdl.await();
                    lock.acquire();

                    TimeUnit.SECONDS.sleep(5);
                    log.info("生成的订单号是: {}",
                            new SimpleDateFormat("HH:mm:ss|SSS").format(new Date()));
                } catch (Exception ignored) {
                } finally {
                    try {
                        lock.release();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        cdl.countDown();
    }

    /**
     * 分布式计数器
     */
    private static void distributeAtomicInt() {
        try {
            DistributedAtomicInteger atomicInteger = new DistributedAtomicInteger(
                    client,
                    // 该节点为永久节点，保存计数器结果
                    "/dist_count",
                    new RetryNTimes(3, 1000));

            AtomicValue<Integer> rc = atomicInteger.add(1);
            log.info("Result: preValue -> {}, postValue -> {}", rc.preValue(), rc.postValue());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static DistributedBarrier distributedBarrier;

    /**
     * 分布式Barrier，原理等同JDK自带的CyclicBarrier，线程同步问题
     */
    private static void distributeBarrier() {
        final String path = "/barrier_path";
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                try {
                    DistributedDoubleBarrier distributedDoubleBarrier =
                            new DistributedDoubleBarrier(client, path, 3);
                    TimeUnit.MILLISECONDS.sleep(Math.round(Math.random() * 3000));
                    log.info("{} enter barrier", Thread.currentThread().getName());
                    // 当指定数量的barrier到达后，开始往下执行
                    distributedDoubleBarrier.enter();

                    TimeUnit.MILLISECONDS.sleep(Math.round(Math.random() * 3000));
                    // 当指定数量的barrier到达后，退出
                    distributedDoubleBarrier.leave();
                    log.info("{} exit barrier", Thread.currentThread().getName());
                } catch (Exception ignored) {
                }
            }, "first-" + i).start();
        }


        try {
            for (int i = 0; i < 3; i++) {
                new Thread(() -> {
                    try {
                        distributedBarrier = new DistributedBarrier(client, path);
                        log.info("{} setBarrier", Thread.currentThread().getName());
                        distributedBarrier.setBarrier();

                        log.info("{} waitOnBarrier", Thread.currentThread().getName());
                        // 等待barrier释放，可以等待同时退出，也可以等待同时开始
                        distributedBarrier.waitOnBarrier();
                        log.info("{} wait over", Thread.currentThread().getName());
                    } catch (Exception ignored) {
                    }
                }, "second-" + i).start();
            }
            TimeUnit.SECONDS.sleep(10);
            // 删除barrier所在的节点
            distributedBarrier.removeBarrier();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
