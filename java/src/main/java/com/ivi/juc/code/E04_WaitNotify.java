package com.ivi.juc.code;

import java.util.concurrent.TimeUnit;

/**
 * 必须结合synchronized使用，因为线程间通信需要使用同一个对象且是锁对象，
 * 否则你notify不知道notify谁，或你notify的线程被其他对象notify了
 */
public class E04_WaitNotify {

    private static final Object lock = new Object();

    public static void main(String[] args) throws InterruptedException {
        demo02();
    }

    private static void demo01() throws InterruptedException {
        new Thread(E04_WaitNotify::block, "wait_1").start();

        new Thread(E04_WaitNotify::block, "wait_2").start();

        TimeUnit.SECONDS.sleep(3);
        new Thread(E04_WaitNotify::block, "notify").start();
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
                    // 获得锁的线程调用wait方法时会先释放锁，然后在锁对象上阻塞，等待被其他线程notify，然后重新拿锁
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
