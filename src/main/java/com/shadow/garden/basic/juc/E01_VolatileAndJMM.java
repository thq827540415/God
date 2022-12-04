package com.shadow.garden.basic.juc;

import java.util.concurrent.TimeUnit;

/**
 * Java线程之间通信由JMM控制，JMM决定一个线程对共享变量的写入对另一个线程可见
 * 问题：
 *  1. 主线程修改flag之后，未将其刷新到主内存，所以Thread-0看不到.
 *  2. 主线程将flag刷新到了主内存，但是Thread-0一直读取自己工作内存中的flag的值，没有去主内存中获取flag最新的值。
 * <p>
 * volatile修饰共享变量：控制了变量在多线程中的可见性
 *  1. 线程中读取的时候，该线程每次读取都会去主内存中去读最新的值，然后将其复制到工作内存中
 *  2. 线程中修改了工作内存中变量的副本，修改之后会立即刷新到主内存
 */
public class E01_VolatileAndJMM {
    private static boolean flag = true;
    private static volatile boolean innerFlag = true;

    public static void main(String[] args) throws InterruptedException {
        new Thread(() -> {
            System.out.println("线程" + Thread.currentThread().getName() + " in");
            while (flag) {
                System.out.println("内部循环进入");
                while (innerFlag) {
                }
                System.out.println("内部循环退出");
            }
            System.out.println("线程" + Thread.currentThread().getName() + " out");
        }).start();

        /*new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(1);
                innerFlag = false;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();*/

        TimeUnit.SECONDS.sleep(1);
        flag = false;
        innerFlag = false;
    }
}
