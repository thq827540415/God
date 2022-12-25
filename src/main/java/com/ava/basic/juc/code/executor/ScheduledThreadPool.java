package com.ava.basic.juc.code.executor;

import com.ava.util.CommonUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ScheduledThreadPool {
    /**
     * 主要用来延迟执行任务，或者定时执行任务。
     * <p>
     * 若任务中抛异常，则会被ScheduledExecutorService内部吞了，通过ScheduleFuture来判断任务是否已经结束.
     * <p>
     * 内部使用的是DelayedWorkQueue，不是DelayQueue。
     */
    private static void demo() {
        // public class ScheduledThreadPoolExecutor extends ThreadPoolExecutor implements ScheduledExecutorService
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
                5,
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());

        // 1. schedule: 延迟执行任务1次
        // command：需要执行的任务、delay：需要延迟的时间、unit：参数2的时间单位
        System.out.println("schedule开始时间为：" + System.currentTimeMillis());
        executor.schedule(() -> {
            System.out.println(System.currentTimeMillis() + " -> schedule开始执行");
            CommonUtils.sleep(1, TimeUnit.SECONDS);
            System.out.println(System.currentTimeMillis() + " -> schedule执行结束");
        }, 1, TimeUnit.SECONDS);


        // 2. scheduleAtFixedRate: 固定的频率执行任务
        // command：需要执行的任务、initialDelay：第一次执行延迟时间、period：连续执行之间的时间间隔，包括任务运行的时间
        final AtomicInteger count1 = new AtomicInteger(1);
        System.out.println("scheduleAtFixedRate开始时间为：" + System.currentTimeMillis());
        executor.scheduleAtFixedRate(() -> {
            int currCnt = count1.getAndIncrement();
            System.out.printf("%s -> scheduleAtFixedRate第%s次开始\n", System.currentTimeMillis(), currCnt);
            CommonUtils.sleep(1, TimeUnit.SECONDS);
            System.out.printf("%s -> scheduleAtFixedRate第%s次结束\n", System.currentTimeMillis(), currCnt);
        }, 1, 1, TimeUnit.SECONDS);


        // 3. scheduleWithFixedDelay: 固定的间隔执行任务. delay: 从任务结束开始计算
        final AtomicInteger count2 = new AtomicInteger(1);
        System.out.println("scheduleWithFixedDelay开始时间为：" + System.currentTimeMillis());
        executor.scheduleWithFixedDelay(() -> {
            int currCnt = count2.getAndIncrement();
            System.out.printf("%s -> scheduleWithFixedDelay第%s次开始\n", System.currentTimeMillis(), currCnt);
            CommonUtils.sleep(1, TimeUnit.SECONDS);
            System.out.printf("%s -> scheduleWithFixedDelay第%s次结束\n", System.currentTimeMillis(), currCnt);
        }, 1, 3, TimeUnit.SECONDS);

        CommonUtils.sleep(5, TimeUnit.SECONDS);
        executor.shutdown();
    }

    public static void main(String[] args) {
        // 创建一个大小无限的线程池。此线程池支持定期以及周期性执行任务的需求。
        Executors.newScheduledThreadPool(3);
        // 只有一个核心线程数
        Executors.newSingleThreadScheduledExecutor();
    }
}

