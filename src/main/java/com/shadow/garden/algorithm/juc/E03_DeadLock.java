package com.shadow.garden.algorithm.juc;

import java.util.concurrent.TimeUnit;

/**
 * 非静态同步方法使用this锁
 */
public class E03_DeadLock implements Runnable {

    int flag = 0;

    final Object oneLock = new Object();
    final Object otherLock = new Object();

    @Override
    public void run() {
        while (true) {
            flag++;
            if (flag % 2 == 0) {
                synchronized (oneLock) {
                    synchronized (otherLock) {
                        System.out.println("Method a() has oneLock and otherLock");
                        doWork();
                    }
                }
            } else {
                synchronized (otherLock) {
                    synchronized (oneLock) {
                        System.out.println("Method b() has otherLock and oneLock");
                        doWork();
                    }
                }
            }
        }
    }

    /**
     * 模拟不释放锁
     */
    private void doWork() {
        try {
            TimeUnit.SECONDS.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        E03_DeadLock deadLock = new E03_DeadLock();

        new Thread(deadLock).start();
        new Thread(deadLock).start();
    }
}
