package com.lumine.bigdata.zookeeper;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class ZKOriginDemo {

    private final static CountDownLatch cdl = new CountDownLatch(1);

    public static void main(String[] args) throws IOException, InterruptedException {
        ZooKeeper zkCli = new ZooKeeper("localhost:2181,localhost:2182,localhost:2183", 2000, watchedEvent -> {
            if (watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected) {
                System.out.println("连接成功");
                cdl.countDown();
            }
        });
        cdl.await();

        System.out.println(zkCli);
    }
}
