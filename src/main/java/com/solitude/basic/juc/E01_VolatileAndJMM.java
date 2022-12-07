package com.solitude.basic.juc;

import com.solitude.util.CommonUtils;

import java.util.concurrent.TimeUnit;

/**
 * 线程如果需要操作主内存的数据，需要先将主内存的数据复制到线程独有的工作内存中，操作完成之后再将其刷新到主内存中。
 * 如果线程A想要看到线程B修改后的数据，需要满足：线程B修改数据后，需要将数据从自己的工作内存中刷新到主内存总，并且A需要去主内存中读取数据。
 * <p>
 * Java线程之间通信由JMM控制
 * 问题：
 *  1. 主线程修改flag之后，未将其刷新到主内存，所以Thread-0看不到.
 *  2. 主线程将flag刷新到了主内存，但是Thread-0一直读取自己工作内存中的flag的值，没有去主内存中获取flag最新的值。
 * <p>
 * volatile修饰的共享变量：控制了变量在多线程中的可见性
 *  1. 每次读取这个变量的时候，会强制从主内存中去读，然后将其复制到工作内存中。
 *  2. 每次修改这个变量的时候，会强制将修改后的工作内存中变量的副本，立即刷新到主内存
 *
 * volatile底层的实现原理是内存屏障Memory Fence
 *  1. 对volatile变量的写指令后会加入写屏障
 *  2. 对volatile变量的读指令前会加入读屏障
 */
public class E01_VolatileAndJMM {
    private static boolean flag = true;
    private static volatile boolean innerFlag = true;

    public static void main(String[] args) {
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

        CommonUtils.sleep(1, TimeUnit.SECONDS);
        flag = false;
        innerFlag = false;
    }
}
