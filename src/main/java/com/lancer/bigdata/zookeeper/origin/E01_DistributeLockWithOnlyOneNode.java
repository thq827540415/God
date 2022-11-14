package com.lancer.bigdata.zookeeper.origin;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 利用节点的唯一性，实现分布式锁，存在羊群效应，多节点抢占锁，属于非公平锁
 */
@Slf4j
public class E01_DistributeLockWithOnlyOneNode {
    private ZooKeeper zkCli;
    private String lockPath;
    private final CountDownLatch cdl = new CountDownLatch(1);
    private CountDownLatch wait;

    public void init() {
        try {
            zkCli = new ZooKeeper("zk01:2181,zk02:2182,zk03:2183", 2000, watchedEvent -> {
                if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    cdl.countDown();
                }
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeDeleted) {
                    wait.countDown();
                }
            });
            cdl.await();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void lock() {
        while (true) {
            String path = "/lock/eph";
            wait = new CountDownLatch(1);

            // 判断是否存在子节点
            try {
                lockPath = zkCli.create(path, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                break;
            } catch (InterruptedException | KeeperException e) {
                try {
                    zkCli.getData(path, true, null);
                    wait.await();
                } catch (InterruptedException | KeeperException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void unlock() {
        try {
            zkCli.delete(lockPath, -1);
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                E01_DistributeLockWithOnlyOneNode lock = new E01_DistributeLockWithOnlyOneNode();
                try {
                    lock.init();
                    lock.lock();
                    log.info("Thread {} get lock successful, sessionID is: {}", Thread.currentThread().getName(), lock.zkCli.getSessionId());
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                    log.info("Thread {} release lock", Thread.currentThread().getName());
                }
            }).start();
        }
    }
}
