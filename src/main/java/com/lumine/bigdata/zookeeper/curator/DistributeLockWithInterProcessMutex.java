package com.lumine.bigdata.zookeeper.curator;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.concurrent.TimeUnit;

@Slf4j
public class DistributeLockWithInterProcessMutex {
    public static void main(String[] args) {
        CuratorFramework client =  createSimple("localhost:2181,localhost:2182,localhost:2183");
        client.start();

        InterProcessMutex lock = new InterProcessMutex(client, "/lock");

        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    lock.acquire();
                    log.info("Thread {} get lock successful, sessionID is: {}", Thread.currentThread().getName(), client.getZookeeperClient().getZooKeeper().getSessionId());
                    TimeUnit.SECONDS.sleep(5);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        lock.release();
                        log.info("Thread {} release lock", Thread.currentThread().getName());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    public static CuratorFramework createSimple(String connStr) {
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
        return CuratorFrameworkFactory.newClient(connStr, retryPolicy);
    }
}
