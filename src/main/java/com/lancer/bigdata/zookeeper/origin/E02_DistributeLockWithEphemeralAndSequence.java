package com.lancer.bigdata.zookeeper.origin;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 利用临时节点 + 序列号实现分布式锁，属于公平锁
 */
@Slf4j
public class E02_DistributeLockWithEphemeralAndSequence {
    private ZooKeeper zkCli;
    private String previewNodePath;
    private String currentNodePath;
    private final CountDownLatch cdl = new CountDownLatch(1);
    private final CountDownLatch wait = new CountDownLatch(1);


    public void init() {
        try {
            zkCli = new ZooKeeper("zk01:2181,zk02:2182,zk03:2183", 2000, watchedEvent -> {
                if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    cdl.countDown();
                }

                if (watchedEvent.getType() == Watcher.Event.EventType.NodeDeleted && watchedEvent.getPath().equals(previewNodePath)) {
                    wait.countDown();
                }
            });
            cdl.await();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void lock() {
        try {
            String path = "/lock/" + zkCli.getSessionId() + "-";

            // 创建临时节点，返回创建节点的值
            currentNodePath = zkCli.create(path, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

            // 判断创建的节点是否是最小的序号节点，如果不是，监听前一个节点
            // List(/lock/sessionId1-000000001, /lock/sessionId2-000000002, ...)
            List<String> zkChildren = zkCli.getChildren("/lock", false)
                    .stream()
                    .map(p -> "/lock/" + p)
                    .sorted(Comparator.comparingLong(p -> Long.parseLong(getNodeSequenceNumber(p, "-"))))
                    .collect(Collectors.toList());

            // 获取当前节点的下标
            int index = zkChildren.indexOf(currentNodePath);

            // 如果不是第一个，则监听前一个节点
            if (index != 0) {
                previewNodePath = zkChildren.get(index - 1);
                zkCli.getData(previewNodePath, true, null);
                // 持续监听
                wait.await();
            }

        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param path     "/lock/sessionId-000000001"
     * @param splitter "-"
     * @return "000000001"
     */
    public String getNodeSequenceNumber(String path, String splitter) {
        String[] fields = path.split(splitter);
        return fields[fields.length - 1];
    }

    public void unlock() {
        try {
            zkCli.delete(currentNodePath, -1);
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                // 多个客户端
                E02_DistributeLockWithEphemeralAndSequence lock = new E02_DistributeLockWithEphemeralAndSequence();
                try {
                    lock.init();
                    lock.lock();
                    log.info("Thread {} get lock successful, sessionID is: {}", Thread.currentThread().getName(), lock.zkCli.getSessionId());
                    TimeUnit.SECONDS.sleep(10);
                    lock.unlock();
                    log.info("Thread {} release lock", Thread.currentThread().getName());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
