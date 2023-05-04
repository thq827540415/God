package com.ivi.bigdata.zookeeper.origin;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;


@Slf4j
public class E01_ZKApi {
    private static final ZooKeeper zkCli;

    static {
        CountDownLatch cdl = new CountDownLatch(1);
        try {
            zkCli = new ZooKeeper("bigdata01:2181,bigdata02:2181,bigdata03:2181", 1000, watchedEvent -> {
                if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    log.info("linked successful!");
                    cdl.countDown();
                }
            });
            cdl.await();

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void createNode() {
        try {
            zkCli.create("/test",
                    "test".getBytes(StandardCharsets.UTF_8),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT);
        } catch (KeeperException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                zkCli.close();
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
        }
    }

    private static void readNode() {
        try {
            log.info("children are {}",
                    zkCli.getChildren("/test",
                            watchedEvent -> {
                                log.info("children have changed!");
                            }));
        } catch (KeeperException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 需要手动递归删除非空节点
     */
    private static void deleteNode() {
    }

    public static void main(String[] args) {
    }
}
