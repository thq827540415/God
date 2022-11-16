package com.lumine.algorithm.juc;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class E99_CompletableFuture {

    /**
     * 小试牛刀
     */
    private static void first() {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CompletableFuture
                .supplyAsync(
                        () -> {
                            System.out.println("Current Thread is daemon ? => " + Thread.currentThread().isDaemon());
                            sleep(1);
                            return 1;
                        }, executorService)
                .thenApply(f -> f * 5)
                .thenAccept(f -> System.out.println("This is accept and result = " + f))
                // 回调函数
                .whenComplete((ignored, throwable) -> System.out.println("Callback Complete!"))
                .exceptionally(
                        f -> {
                            f.printStackTrace();
                            return null;
                        });

        System.out.println("Current thread is main");
        executorService.shutdown();
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
        Integer result = first.applyToEither(second, f -> f + 1).join();
        System.out.println(result);
    }

    /**
     * and关系，先完成的先等待，等待其他分支任务
     * thenCombine(BiFunction)、thenAcceptBoth(BiConsumer)、runAfterBoth(Runnable)
     */
    private static void third() {
        Integer result = CompletableFuture
                .supplyAsync(
                        () -> {
                            System.out.println(Thread.currentThread().getName() + "\t" + "come in");
                            sleep(3);
                            return 3;
                        })
                .thenCombine(
                        CompletableFuture
                                .supplyAsync(
                                        () -> {
                                            System.out.println(Thread.currentThread().getName() + "\t" + "come in");
                                            sleep(2);
                                            return 2;
                                        }),
                        (x, y) -> {
                            System.out.println(Thread.currentThread().getName() + "\t" + "come int");
                            return x + y;
                        })
                .thenCombine(
                        CompletableFuture
                                .supplyAsync(
                                        () -> {
                                            System.out.println(Thread.currentThread().getName() + "\t" + "come int");
                                            sleep(1);
                                            return 1;
                                        }),
                        (x, y) -> {
                            System.out.println(Thread.currentThread().getName() + "\t" + "come int");
                            return x + y;
                        })
                .join();
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

        // 1.
        long start = System.currentTimeMillis();
        strings
                .stream()
                .map(
                        str -> {
                            sleep(2);
                            return str + "123";
                        })
                .collect(Collectors.toList());
        long end = System.currentTimeMillis();
        System.out.println("normal cost " + (end - start) + " ms");

        // 2.
        start = System.currentTimeMillis();
        strings
                .stream()
                .map(
                        str -> CompletableFuture
                                .supplyAsync(() -> {
                                    sleep(2);
                                    return str + "123";
                                }))
                .collect(Collectors.toList())
                .stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        end = System.currentTimeMillis();
        System.out.println("completable cost " + (end - start) + " ms");
    }

    private static void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        demo();
    }
}
