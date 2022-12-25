package com.ava.basic.juc.code.executor;

import com.ava.util.CommonUtils;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executor接口主要包含三个部分：
 * 1. Runnable接口或Callable接口
 * 2. Executor框架中有两个关键的类实现了ExecutorService：ThreadPoolExecutor和ScheduleThreadPoolExecutor
 * 3. 异步计算结果相关的Future接口和FutureTask类
 */
public class MyThreadPool {
    public static void main(String[] args) {
    }

    /**
     * ThreadPoolExecutor
     * 对于CPU密集型应用，应尽可能少的线程数，如配置cores + 1个线程的线程池。
     * 对于IO密集型应用，不能让CPU闲着，应尽可能多的线程数，如配置cores * 2个线程的线程池。
     * <p>
     * Ncpu为CPU的数量，Ucpu为CPU的使用率，W / C为等待时间与计算时间的比例
     * 最优线程池大小：NThreads = NCpu * UCpu * (1 + WaitTime / ComputeTime)
     */
    private static void threadpoolExecutor() {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                // 核心线程数，线程池中至少保证存活的线程数，不会初始化好。
                3,
                // 线程池允许创建的最大线程数
                5,
                // 线程池的工作线程空闲后，保持存活的时间
                10,
                TimeUnit.SECONDS,
                // 用户缓存待处理任务的阻塞队列
                new ArrayBlockingQueue<>(10),
                // 线程池中创建线程的工厂
                Executors.defaultThreadFactory(),
                // 线程池无法处理新来的任务时的拒绝策略
                new ThreadPoolExecutor.AbortPolicy()) {

            /**
             * @param t the thread that will run task {@code r}
             * @param r the task that will be executed
             */
            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                System.out.println(t.getName() + "开始执行任务：" + r.toString());
            }


            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                System.out.println(Thread.currentThread().getName() + "任务：" + r.toString() + "执行完毕！");
            }

            @Override
            protected void terminated() {
                System.out.println(Thread.currentThread().getName() + "关闭线程池。");
            }
        };

        AtomicInteger threadNum = new AtomicInteger(1);
        // 自定义创建线程的工厂
        new ThreadFactory() {
            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread t = new Thread(r);
                t.setName("自定义线程-" + threadNum.getAndIncrement());
                return new Thread(r);
            }
        };


        // 直接抛出异常
        new ThreadPoolExecutor.AbortPolicy();
        // 在当前调用者的线程中运行任务，即谁丢来的任务，谁处理
        new ThreadPoolExecutor.CallerRunsPolicy();
        // 丢弃队列中最老的一个任务，即丢弃队列头部的一个任务，然后放入当前传入的任务
        new ThreadPoolExecutor.DiscardOldestPolicy();
        // 不处理，直接丢弃
        new ThreadPoolExecutor.DiscardPolicy();
        // 自定义拒绝策略
        new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                System.out.println("无法处理的任务：" + r.toString());
            }
        };


        // 线程池会提前把核心线程都创建好，并启动
        threadPoolExecutor.prestartAllCoreThreads();

        // 核心线程是否也会被空闲销毁，默认为false
        threadPoolExecutor.allowCoreThreadTimeOut(false);

        // 线程池的execute方法
        // 1. 判断线程池中运行的线程数是否小于核心线程数。若否，执行2
        // 2. 尝试将任务添加到工作队列中。若无法添加，执行3
        // 3. 判断线程池中运行的线程数是否小于最大线程数。若否，调用拒绝策略


        /*
         * 线程池中的两个关闭方法shutdown和shutdownNow
         * 线程池会遍历内部的工作线程，然后调用每个工作线程的interrupt方法给线程发送中断信号。
         * 当线程内部有无限循环的，最好在循环内部检测一下线程的中断信号。
         * shutdown后不会再接收新任务到队列中
         * shutdownNow会将在队列中等待的任务移除。
         */
        threadPoolExecutor.shutdown();
    }


    /**
     * 线程池最好不用Executors去创建
     */
    private static void executors() {
        // 1. 只有一个线程在工作，任务的执行顺序与任务的提交顺序一致。
        // 内部使用了无限容量的LinkedBlockingQueue，如果任务较多，会导致OOM
        Executors.newSingleThreadExecutor();

        // 2. 创建固定大小的线程池，每提交一任务就创建一个线程。
        // 内部使用了无限容量的LinkedBlockingQueue，如果任务较多，会导致OOM
        Executors.newFixedThreadPool(3);

        // 3. 创建一个可缓存的线程池，每60s会回收空闲线程，最大线程数为Integer.MAX_VALUE。
        // 内部使用了SynchronousQueue同步队列来缓存任务，如果处理的任务比较耗时，任务来的速度也比较快，会创建太多的线程引发OOM
        Executors.newCachedThreadPool();


    }

    private static void future() {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // 当有返回值时，传入的是Callable实例，返回的实际上是个FutureTask
        // Future需要结合ExecutorService来使用
        Future<Integer> future = executor.submit(() -> {
            CommonUtils.sleep(2, TimeUnit.SECONDS);
            return 1;
        });

        try {
            // 阻塞当前线程。虽然任务比较耗时，但是我最多只能等待1s
            // future.get(1, TimeUnit.SECONDS);

            // 取消在执行的任务，是否发送中断信号
            future.cancel(false);
            System.out.println(future.isCancelled());
            System.out.println(future.isDone());
            System.out.println(future.get());
        } catch (Exception e) {
            e.printStackTrace();
        }


        // FutureTask可以通过单个线程来执行
        FutureTask<Integer> futureTask = new FutureTask<>(() -> {
            CommonUtils.sleep(2, TimeUnit.SECONDS);
            return 1;
        });

        new Thread(futureTask).start();

        try {
            System.out.println("FutureTask's result: " + futureTask.get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        executor.shutdown();
    }

    /**
     * ExecutorCompletionService创建的时候会传入一个线程池，调用submit方法传入需要执行的任务，任务由内部的线程池来处理。
     * ExecutorCompletionService内部有个阻塞队列，任意一个任务完成之后，会将任务的执行结果(Future类型)放入阻塞队列中，
     * 然后其他线程可以调用他的take、poll方法从这个阻塞队列中获取一个已经完成的任务，获取任务返回结果的顺序和任务执行完成的先后顺序一致，
     * 所以最先完成的任务会先返回。
     * <p>
     * 批量执行异步任务
     */
    private static void executorCompletionService() {
        // todo
    }
}
