package com.shadow.garden.basic.juc;

import java.util.concurrent.TimeUnit;

/**
 * 得结合synchronized使用
 */
public class E01_WaitNotify {

    private static final Object lock = new Object();

    public static void main(String[] args) throws InterruptedException {
        demo02();
    }

    private static void demo01() throws InterruptedException {
        new Thread(E01_WaitNotify::block, "wait_1").start();

        new Thread(E01_WaitNotify::block, "wait_2").start();

        TimeUnit.SECONDS.sleep(3);
        new Thread(E01_WaitNotify::block, "notify").start();
    }

    /**
     * 同步代码块
     */
    private static void block() {
        synchronized (lock) {
            String currThread = Thread.currentThread().getName();
            if (!"notify".equals(currThread)) {
                System.out.printf("Thread %s enters the wait\n", currThread);
                try {
                    // 获得锁的线程调用wait方法会在锁对象上等待，且该线程释放锁
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.printf("Thread %s has been notified\n", currThread);
            } else {
                // 该线程会唤醒一条在锁对象上等待的线程（FIFO），notifyAll则会唤醒所有线程
                lock.notifyAll();
            }
        }
    }


    /**
     * 生产者消费者案例
     */
    private static void demo02() {
        Object a = new Object();
        class Message {
            String name;
            int age;
            boolean flag = false;
        }

        Message msg = new Message();
        new Thread(() -> {
            int count = 0;
            while (true) {
                synchronized (a) {
                    if (msg.flag) {
                        try {
                            a.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (count++ == 0) {
                        msg.name = "zs";
                        msg.age = 1;
                    } else {
                        msg.name = "ls";
                        msg.age = 2;
                    }
                    msg.flag = true;
                    count %= 2;
                    a.notify();
                }
            }
        }, "producer").start();

        new Thread(() -> {
            while (true) {
                synchronized (a) {
                    if (!msg.flag) {
                        try {
                            a.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    System.out.println(msg.name + ", " + msg.age);
                    msg.flag = false;
                    a.notify();
                }
            }
        }, "consumer").start();
    }
}
