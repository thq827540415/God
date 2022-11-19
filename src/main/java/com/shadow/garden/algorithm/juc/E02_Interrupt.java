package com.shadow.garden.algorithm.juc;

import java.util.concurrent.TimeUnit;

public class E02_Interrupt {
    public static void main(String[] args) throws InterruptedException {
        Thread a = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                /*try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }*/
                System.out.println("=====");
            }
            // 重置状态
            Thread.interrupted();
        }, "a");
        a.start();

        TimeUnit.SECONDS.sleep(2);
        a.interrupt();
        TimeUnit.SECONDS.sleep(1);
        System.out.println(a.isInterrupted());
    }
}
