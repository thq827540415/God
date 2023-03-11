package com.ava.basic.juc.code.async;

import com.ava.util.CommonUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class MyCompletableFuture {

    private static Future<String> aAsyncMethod() {

        CompletableFuture<String> future = new CompletableFuture<>();

        new Thread(() -> {
            try {
                CommonUtils.sleep(3, TimeUnit.SECONDS);
                // future完成时返回值
                future.complete("complete");
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }).start();

        return future;
    }


    /**
     * 小试牛刀
     */
    private static void first() {
        // ExecutorService executorService = Executors.newFixedThreadPool(2);
        ExecutorService executorService = Executors.newCachedThreadPool();
        CompletableFuture
                .supplyAsync(
                        () -> {
                            info("Current Thread is daemon ? => " + Thread.currentThread().isDaemon());
                            CommonUtils.sleep(100, TimeUnit.MILLISECONDS);
                            return 1;
                        }, executorService)
                // 上面执行完后，再起一个异步任务
                .thenCompose(
                        f -> CompletableFuture
                                .supplyAsync(
                                        () -> {
                                            info("do the compose. f = " + f);
                                            CommonUtils.sleep(100, TimeUnit.MILLISECONDS);
                                            return 2;
                                        }, executorService))
                // 等价于上面
                .thenApplyAsync(
                        f -> {
                            info("the same as compose. f = " + f);
                            CommonUtils.sleep(100, TimeUnit.MILLISECONDS);
                            return 3;
                        }, executorService)
                // 上一步完成后执行下一步
                .thenApply(
                        f -> {
                            info("do the compute.");
                            return f * 5;
                        })
                // 上一步完成后执行下一步
                .thenAccept(f -> info("This is accept and result = " + f))
                // 回调函数
                .whenComplete((ignored, throwable) -> info("Callback Complete!"))
                .exceptionally(
                        f -> {
                            f.printStackTrace();
                            return null;
                        });

        info("Current thread is main");
        // executorService.shutdown();
    }

    /**
     * or关系，谁先计算完就用谁
     * applyToEither(Function)、acceptEither(Consumer)、runAfterEither(Runnable)
     */
    private static void second() {
        CompletableFuture<Integer> first = CompletableFuture
                .supplyAsync(
                        () -> {
                            sleep(1);
                            return 1;
                        });
        CompletableFuture<Integer> second = CompletableFuture
                .supplyAsync(
                        () -> {
                            sleep(2);
                            return 2;
                        });
        // Integer result = first.applyToEither(second, f -> f + 1).join();

        Integer result = (Integer) CompletableFuture.anyOf(first, second).join();

        System.out.println(result);
    }

    /**
     * and关系，先完成的先等待，等待其他分支任务
     * thenCombine(BiFunction)、thenAcceptBoth(BiConsumer)、runAfterBoth(Runnable)
     */
    private static void third() {
        CompletableFuture<Integer> cf1 = CompletableFuture
                .supplyAsync(
                        () -> {
                            info("cf1 come in");
                            sleep(3);
                            return 3;
                        });
        CompletableFuture<Integer> cf2 = CompletableFuture
                .supplyAsync(
                        () -> {
                            info("cf2 come in");
                            sleep(2);
                            return 2;
                        });

        Integer result = cf1
                .thenCombine(
                        cf2,
                        (x, y) -> {
                            info("combine logic come in");
                            return x * y;
                        })
                .join();

        // join会等到两者都执行完后，才开始往下执行
        // CompletableFuture.allOf(cf1, cf2).join();
        System.out.println(result);
    }

    /**
     * 前面的步骤出现异常，依旧可以执行handle中的逻辑，但是后面的逻辑不可执行
     */
    private static void fourth() {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CompletableFuture
                .supplyAsync(
                        () -> {
                            int k = 10 / 0;
                            System.out.println("This is supply");
                            return 10;
                        }, executorService)
                .thenApply(
                        f -> {
                            System.out.println("This is apply");
                            return f + 1;
                        })
                .handle(
                        (f, e) -> {
                            System.out.println("This is handle");
                            return f + 2;
                        })
                .thenAccept(f -> System.out.println("This is accept"))
                .whenComplete(
                        (integer, throwable) -> {
                            sleep(1);
                            System.out.println("This is complete " + integer);
                        })
                .exceptionally(
                        f -> {
                            f.printStackTrace();
                            return null;
                        });

        executorService.shutdown();
    }

    /**
     * 批量执行异步任务，先将执行完成的任务的执行结果的Future对象放入阻塞队列中
     */
    private static void fifth() {
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        LinkedBlockingQueue<Future<Integer>> queue = new LinkedBlockingQueue<>(3);
        // 默认使用无界的LinkedBlockingQueue
        ExecutorCompletionService<Integer> ecs = new ExecutorCompletionService<>(executorService, queue);


        ecs.submit(
                () -> {
                    sleep(5);
                    return 1;
                });
        ecs.submit(
                () -> {
                    sleep(2);
                    return 2;
                });
        ecs.submit(
                () -> {
                    sleep(4);
                    return 3;
                });

        for (int i = 0; i < 3; i++) {
            final Integer result;
            try {
                // 如果take不到，就会一直阻塞
                result = ecs.take().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            executorService.execute(() -> System.out.println(result));
        }

        executorService.shutdown();
    }

    private static void demo() {
        List<String> strings = Arrays.asList("abc", "def", "ghi");

        // 1. stream
        long start = System.currentTimeMillis();
        strings
                .stream()
                .map(str -> {
                    sleep(2);
                    return str + "123";
                })
                .collect(Collectors.toList());
        System.out.println("normal cost " + (System.currentTimeMillis() - start) + " ms");


        // 2. 并行流
        start = System.currentTimeMillis();
        strings
                .parallelStream()
                .map(str -> {
                    sleep(2);
                    return str + "123";
                })
                .collect(Collectors.toList());
        System.out.println("normal parallel cost " + (System.currentTimeMillis() - start) + " ms");

        // 3. CF
        start = System.currentTimeMillis();
            strings.stream()
                .map(str ->
                        CompletableFuture
                                .supplyAsync(() -> {
                                    sleep(2);
                                    return str + "123";
                                }))
                .collect(Collectors.toList()).stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        System.out.println("completable cost " + (System.currentTimeMillis() - start) + " ms");
    }

    private static void sleep(int seconds) {
        CommonUtils.sleep(seconds, TimeUnit.SECONDS);
    }

    private static void info(String msg) {
        System.out.println(Thread.currentThread().getName() + " | " + msg);
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        third();
    }
}
