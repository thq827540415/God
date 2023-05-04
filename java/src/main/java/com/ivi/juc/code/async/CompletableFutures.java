package com.ivi.juc.code.async;

import com.ivi.juc.code.CommonUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @Author lancer
 * @Date 2023/3/11 20:43
 * @Description <a href="https://tech.meituan.com/2022/05/12/principles-and-practices-of-completablefuture.html">CF</a>
 * 底层也使用了CAS解决并发问题
 */

public class CompletableFutures {
    static final ExecutorService executor = Executors.newFixedThreadPool(5);

    public static void main(String[] args) {
        oneDependency();
    }

    /**
     * 创建CompletableFuture主要的3种方式
     */
    static void createCF() {
        // todo 1. 使用静态方法runAsync()、supplyAsync()发起异步调用
        CompletableFuture<String> cf1 = CompletableFuture.supplyAsync(() -> "result1", executor);


        // todo 2. 使用静态方法completedFuture()直接创建一个已完成状态的CompletableFuture
        CompletableFuture<String> cf2 = CompletableFuture.completedFuture("result2");


        // todo 3. 先初始化一个未完成的CF，然后通过complete()、completeExceptionally()，完成该CF
        // 典型的使用场景：将回调方法转为CF，然后通过CF的能力进行编排
        CompletableFuture<String> cf3 = new CompletableFuture<>();
        // 如下只是进行任务的编排，并没有开始执行
        cf3
                // 等待cf3的完成后的异步回调函数，定义新的执行逻辑，然后返回新的cf
                .thenApplyAsync(
                        str -> {
                            sleep(2);
                            info("thenApplyAsync异步回调函数执行：" + str);
                            return str;
                        },
                        executor)
                .thenApply(
                        str -> {
                            sleep(1);
                            info("执行");
                            return str;
                        })
                .whenComplete((v, ex) -> info("v = " + v + ", ex = " + extractRealException(ex)));

        // 正常完成：执行complete后，可理解为第一个CF完成后，开始执行接下来定义的任务逻辑
        info("cf3完成：" + cf3.complete("result3"));

        executor.shutdown();
    }


    /**
     * 只依赖一个CF，可通过thenApply()、thenAccept()、thenCompose()等方法实现
     * <p>
     * 不加Async的回调方法均为同步回调
     */
    static void oneDependency() {
        CompletableFuture
                // 先创建一个完成的CompletableFuture对象
                .completedFuture("cf")
                .thenApply(
                        str -> {
                            sleep(1);
                            info("this is thenApply");
                            return str + "1";
                        })
                .thenCompose(
                        str -> {
                            info("这里是thenCompose");
                            // compose需要重新传入一个异步调用
                            return CompletableFuture
                                    .supplyAsync(
                                            () -> {
                                                sleep(1);
                                                info("这是thenCompose的异步调用");
                                                return str + "2";
                                            },
                                            executor);
                        })
                .thenApply(
                        str -> {
                            sleep(1);
                            info("这是thenCompose后的thenApply的调用");
                            return str + "3";
                        })
                // 等同于上面的thenCompose
                .thenApplyAsync(
                        str -> {
                            sleep(1);
                            info("这是thenApplyAsync的异步调用");
                            return str + "4";
                        },
                        executor)
                .thenAccept(CompletableFutures::info)
                .join();
        executor.shutdown();
    }


    /**
     * 依赖两个CF
     * <p>
     * and关系，可通过thenCombine(Function)、thenAcceptBoth(Consumer)、runAfterBoth(Runnable)等方法实现
     * <p>
     * or关系，可通过applyToEither(...)、acceptEither(...)、runAfterEither(...)等方法实现
     */
    static void twoDependencies() {
        CompletableFuture<Integer> first = CompletableFuture
                .supplyAsync(
                        () -> {
                            sleep(2);
                            info("first完成");
                            return 1;
                        },
                        executor);
        CompletableFuture<Integer> second = CompletableFuture
                .supplyAsync(
                        () -> {
                            sleep(1);
                            info("second完成");
                            return 2;
                        },
                        executor);

        // 当两个都完成了之后，调用后面的逻辑
        info("and的最终结果为：" + first.thenCombine(second, Integer::sum).join());

        // 当两者其中一个完成了之后，调用后面的逻辑
        info("or的最终结果为：" + first.acceptEither(second, num -> info("" + num)));
        executor.shutdown();
    }


    /**
     * 当依赖多个CF，可通过allOf()或anyOf()实现
     */
    static void mutilDependencies() {
        class Task {
            final String name;

            public Task(String name) {
                this.name = name;
            }

            private int rand() {
                Random r = new Random();
                return r.nextInt(3) + 1;
            }

            CompletableFuture<?>[] getTask() {
                CompletableFuture<Integer> first = CompletableFuture
                        .supplyAsync(
                                () -> {
                                    sleep(rand());
                                    info(this + "中的first完成");
                                    return 1;
                                },
                                executor);
                CompletableFuture<Integer> second = CompletableFuture
                        .supplyAsync(
                                () -> {
                                    sleep(rand());
                                    info(this + "中的second完成");
                                    return 2;
                                },
                                executor);
                CompletableFuture<Integer> third = CompletableFuture
                        .supplyAsync(
                                () -> {
                                    sleep(rand());
                                    info(this + "中的third完成");
                                    return 3;
                                },
                                executor);
                return Arrays.asList(first, second, third).toArray(new CompletableFuture<?>[0]);
            }

            @Override
            public String toString() {
                return name;
            }
        }

        Task task = new Task("allOf");
        CompletableFuture<?>[] futures = task.getTask();

        // 当所有的任务完成后才执行
        CompletableFuture
                // allOf返回Void
                .allOf(futures)
                .thenAccept(
                        vo -> {
                            Integer n1 = (Integer) futures[0].join();
                            Integer n2 = (Integer) futures[1].join();
                            Integer n3 = (Integer) futures[2].join();
                            info("最终的结果为：" + (n1 + n2 + n3));
                        })
                .join();

        System.out.println("========================================");

        // 当所有任务的中的其中一个完成后，就返回
        CompletableFuture
                // allOf返回Object，最先完成的任务的所返回的类型
                .anyOf(new Task("anyOf").getTask())
                .thenAccept(res -> info("最终的结果为：" + res));

        executor.shutdown();
    }


    /**
     * 批量执行异步任务，先将执行完成的任务的执行结果的Future对象放入阻塞队列中
     */
    static void completionService() {
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
                // 如果get不到，就会一直阻塞
                result = ecs.take().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
            executorService.execute(() -> info("" + result));
        }

        executorService.shutdown();
    }


    static void parallelSteamCompareToCF() {
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
        List<CompletableFuture<String>> futures = strings.stream()
                .map(str ->
                        CompletableFuture
                                .supplyAsync(
                                        () -> {
                                            sleep(2);
                                            return str + "123";
                                        }))
                .collect(Collectors.toList());
        // 需要再创建一个Stream获取结果
        futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
        System.out.println("completable cost " + (System.currentTimeMillis() - start) + " ms");
    }


    /**
     * 代码执行在哪个线程上
     * <p>
     * 不带Async后缀的同步"回调方法"（为上一步执行完后的回调函数）:
     * 1. 如果注册时被依赖的操作已经完成，则直接由当前线程执行。
     * 2. 如果注册时被依赖的操作还未完成，则由回调线程池中的线程执行。
     */
    static void whoExecuteJob() {
        CompletableFuture<Void> cf = CompletableFuture
                .runAsync(
                        () -> {
                            info("supplyAsync执行");
                            sleep(2);
                        },
                        executor);

        // 如果cf中的任务已经执行完毕并返回，则该thenAccept直接由当前线程执行（此时为main）
        // 否则，将会由执行以上业务操作的executor中的线程执行。
        // 典型场景为：如果使用的是基于NIO的异步RPC，则同步回调会运行在IO线程上，而NIO整个服务只有一个IO线程池，
        // 此时需要保证同步回调中不能有阻塞等待耗时过长的逻辑。
        cf.thenAccept(value -> info("thenAccept执行"));


        // 使用CF自带的ForkJoinPool中的共用线程池CommonPool，大小为CPU核数 - 1
        cf.thenAcceptAsync(value -> info("thenAcceptAsync自带线程池执行"));

        // 异步回调方法使用指定线程池（推荐），做线程池隔离
        cf.thenAcceptAsync(value -> info("thenAcceptAsync指定线程池执行"), executor);

        sleep(3);
        executor.shutdown();
        System.out.println("main线程执行到这了！");
    }


    /**
     * 对线程池的循环引用会导致死锁(某种定义上的死锁，jconsole等工具没有检查到死锁)
     * <p>
     * 当executor被打满，子任务请求线程时进入阻塞队列，但是父任务的完成又依赖子任务，此时子任务得不到线程，故父任务无法完成
     * <p>
     * 为了解决该问题，需要将父子任务做线程池隔离
     */
    static void loopRefDeadlock() {
        ExecutorService e = Executors.newFixedThreadPool(1);

        System.out.println(
                CompletableFuture
                        .supplyAsync(
                                () -> {
                                    info("parent");
                                    return CompletableFuture
                                            .supplyAsync(
                                                    () -> {
                                                        info("child");
                                                        return "child";
                                                    },
                                                    e)
                                            .join();
                                }
                                , e)
                        .join());

        executor.shutdown();
        System.out.println("main线程执行到这了！");
    }


    /**
     * CF在回调方法中对异常进行了包装，大部分异常会封装成CompletionException后抛出，真正的异常存储在Throwable中的cause属性中。
     * 但有些时候会直接返回真正的异常，最好使用工具类提取异常。
     */
    static void exceptionCapture() {
        CompletableFuture
                .completedFuture("first")
                .thenAcceptAsync(
                        msg -> {
                            sleep(1);
                            info("抛出异常");
                            throw new RuntimeException(msg);
                        },
                        executor)
                .thenAccept(vo -> info("异常和handle之间的逻辑执行！"))
                // handle方法会捕获上一个CF发生的异常，会让后面的逻辑正常执行
                .handle(
                        (unused, throwable) -> {
                            info("handle, ex = " + throwable);
                            sleep(1);
                            return null;
                        })
                .thenAcceptAsync(val -> info("handle之后的执行逻辑！"), executor)
                .exceptionally(
                        ex -> {
                            info("exceptionally, ex: " + ex + ", util: " + extractRealException(ex));
                            return null;
                        })
                .join();


        System.out.println("========================================================");

        CompletableFuture
                .completedFuture("second")
                .thenAcceptAsync(
                        msg -> {
                            sleep(1);
                            info("抛出异常");
                            throw new RuntimeException(msg);
                        },
                        executor)
                .thenAccept(vo -> info("异常和whenComplete之间的逻辑执行！"))
                // 上一个CF是否完成，没完成则抛异常
                .whenComplete((vo, err) -> info("whenComplete, ex = " + err))
                .thenAccept(val -> info("whenComplete之后的执行逻辑！"))
                .exceptionally(
                        ex -> {
                            info("exceptionally, ex: " + ex + ", util: " + extractRealException(ex));
                            return null;
                        })
                .join();

        System.out.println("========================================================");

        CompletableFuture<String> root = new CompletableFuture<>();
        root
                .whenComplete(
                        (v, e) -> {
                            if (e != null) {
                                info("异常完成, v = " + v + ", ex = " + e);
                            } else {
                                info("正常完成, v = " + v);
                            }
                            sleep(1);
                        })
                .whenCompleteAsync(
                        (v, e) -> {
                            if (e != null) {
                                info("异常完成, v = " + v + ", ex = " + e);
                            } else {
                                info("正常完成, v = " + v);
                            }
                        },
                        executor);
        root.completeExceptionally(new IOException());


        executor.shutdown();
        System.out.println("main线程执行到这了！");
    }

    static void sleep(int seconds) {
        CommonUtils.sleep(seconds, TimeUnit.SECONDS);
    }

    static void info(String msg) {
        System.out.println(Thread.currentThread().getName() + " | " + msg);
    }

    static Throwable extractRealException(Throwable throwable) {
        if (throwable instanceof CompletionException || throwable instanceof ExecutionException) {
            if (throwable.getCause() != null) {
                return throwable.getCause();
            }
        }
        return throwable;
    }
}
