package com.shadow.garden.basic.juc;

import java.util.concurrent.TimeUnit;

public class E02_Interrupt {
    private static volatile boolean exit = false;
    /**
     * 通过一个变量控制线程中断
     */
    private static void interruptByVariable() throws InterruptedException {
        new Thread(() -> {
            while (true) {
                if (exit) {
                    break;
                }
            }
        }).start();
        TimeUnit.SECONDS.sleep(3);
        exit = true;
    }

    /**
     * 通过线程自带的中断标志控制
     */
    private static void interruptByInterruptSign() throws InterruptedException {
        Thread t = new Thread(() -> {
            while (true) {
                // 可以通过线程实例的isInterrupted()获取线程的中断标志
                if (Thread.currentThread().isInterrupted()) {
                    // Thread.interrupted() -> currentThread().isInterrupted(true)
                    // 返回当前线程的中断标志，同时清除中断标志（置为false）
                    System.out.println("first ->" + Thread.interrupted());
                    System.out.println("second ->" + Thread.interrupted());
                    System.out.println("third ->" + Thread.interrupted());
                    break;
                }
            }
        });
        t.start();
        TimeUnit.SECONDS.sleep(3);
        System.out.println(t.isInterrupted());
        t.interrupt();
        System.out.println(t.isInterrupted());
    }

    /**
     * 中断阻塞状态中的线程
     */
    private static void interruptByInterruptSign2() throws InterruptedException {
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    TimeUnit.SECONDS.sleep(30);
                } catch (InterruptedException e) {
                    // 抛出异常后，会清除线程内部的中断标志（中断标志置为false）
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                }
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("退出成功");
                    break;
                }
            }
        });
        t.start();
        TimeUnit.SECONDS.sleep(3);
        // 中断sleep中的线程，抛出InterruptedException
        t.interrupt();
    }

    public static void main(String[] args) throws InterruptedException {
    }
}
