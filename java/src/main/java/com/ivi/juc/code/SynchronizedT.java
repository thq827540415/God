package com.ivi.juc.code;

import java.util.concurrent.TimeUnit;

/**
 * 是对一个对象加锁，去竞争这把对象锁
 */
public class SynchronizedT {

    synchronized void a() {
        System.out.println("a");
        CommonUtils.sleep(3, TimeUnit.SECONDS);
    }

    void b() {
        synchronized (this) {
            System.out.println("b");
            CommonUtils.sleep(2, TimeUnit.SECONDS);
        }
    }

    public static void main(String[] args) {
        SynchronizedT lock = new SynchronizedT();
        new Thread(lock::a).start();
        new Thread(lock::b).start();
    }
}
