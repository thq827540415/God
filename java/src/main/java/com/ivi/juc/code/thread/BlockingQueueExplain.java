package com.ivi.juc.code.thread;

import com.ivi.juc.code.CommonUtils;
import lombok.AllArgsConstructor;
import lombok.ToString;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 如果队列满的，那么将阻塞该线程，暂停添加数据
 * 如果队列空的，那么将阻塞该线程，暂停取出数据
 * <p>
 * put(e)、take()一直阻塞
 * <p>
 * add(e)、remove() 会直接抛异常
 * <p>
 * offer(e)、poll() 直接返回特殊值：offer -> false/true; poll -> null/具体的值
 * <p>
 * offer(e, timeout, unit)、poll(timeout, unit) 超时阻塞
 * <p>
 * 如果是无界阻塞队列，队列不可能会出现满的情况，所以使用put或offer方法永远不会被阻塞
 */
public class BlockingQueueExplain {

    private static volatile boolean flag = true;

    /**
     * 1. 一个用数组实现的有界阻塞队列。
     * <p>
     * 2. 线程阻塞的实现是通过{@link ReentrantLock}来完成的。
     * <p>
     * 3. 数据的插入与取出共用同一个锁，因此ArrayBlockingQueue并不能同时进行生产、消费。
     */
    private static void arrayBlockingQueue() {
        // 使用非公平锁的ArrayBlockingQueue -> default
        final ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(2, false);

        new Thread(() -> {
            while (flag || !queue.isEmpty()) {
                try {
                    CommonUtils.sleep(500, TimeUnit.MILLISECONDS);

                    String msg = queue.take();
                    System.out.printf("%s获取到的消息为%s\n", Thread.currentThread().getName(), msg);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        for (int i = 1; i <= 5; i++) {
            try {
                String msg = String.format("this is %s", i);
                queue.put(msg);
                System.out.printf("%s发送消息为%s\n", Thread.currentThread().getName(), msg);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        flag = false;
    }


    /**
     * 1. 一个用单链表组成的有界/无界阻塞队列。
     * <p>
     * 2. 不指定容量大小时，一旦数据生产速度大于消费速度，系统内存将可能被消耗殆尽。
     * <p>
     * 3. 用于阻塞生产者、消费者的锁是两个（锁分离），因此生产和消费是可以同时进行的。
     * <p>
     * 4. Executors.newFixedThreadPool使用了这个队列。
     */
    private static void linkedBlockingQueue() {
        // 用法等同于ArrayBlockingQueue，建议使用时指定容量
        final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(2);
    }


    /**
     * 1. 一个支持优先级排序的无界阻塞队列。
     * <p>
     * 2. 内部使用数组存储数据，会自动进行扩容。
     * <p>
     * 3. 元素实现Comparable接口或者初始化队列时指定Comparator
     */
    private static void priorityBlockingQueue() {
        @ToString
        @AllArgsConstructor
        class Msg {
            int priority;
            String msg;
        }
        // 默认容量为11
        final PriorityBlockingQueue<Msg> queue =
                new PriorityBlockingQueue<>(11, (m1, m2) -> m2.priority - m1.priority);

        new Thread(() -> {
            while (flag || !queue.isEmpty()) {
                try {
                    CommonUtils.sleep(500, TimeUnit.MILLISECONDS);
                    Msg msg = queue.take();
                    System.out.printf("%s获取到的消息为%s\n", Thread.currentThread().getName(), msg);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        for (int i = 1; i <= 5; i++) {
            queue.put(new Msg(i, i + ""));
        }
        flag = false;
    }


    /**
     * 1. 一个不存储元素的同步阻塞队列，与其他阻塞队列不同，SynchronousQueue没有容量。
     * <p>
     * 2. 每一个put操作必须要等待一个take操作，否则不能继续添加元素，反之亦然。
     * <p>
     * 3. Executors.newCachedThreadPool()中用到了这个队列。
     * <p>
     * 4. SynchronousQueue的吞吐量高于LinkedBlockingQueue和ArrayBlockingQueue
     */
    private static void synchronousQueue() {
        // Creates a SynchronousQueue with non-fair access policy
        // fair ? new TransferQueue<E>() : new TransferStack<E>();
        final SynchronousQueue<String> queue = new SynchronousQueue<>(false);

        new Thread(() -> {
            try {
                // put时就会阻塞
                queue.put("msg");
                System.out.println("take成功");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

        try {
            CommonUtils.sleep(3, TimeUnit.SECONDS);
            System.out.printf("调用take获取并移除元素，%s\n", queue.take());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 1. 一个使用PriorityQueue实现的，支持延时获取元素的无界阻塞队列。
     * <p>
     * 2. 队列中的元素必须实现Delayed接口。
     */
    private static void delayQueue() {
        @ToString
        @AllArgsConstructor
        class Msg implements Delayed {
            int priority;
            String msg;
            long sendTimeMs;

            /**
             * 如果小于0则继续延迟
             */
            @Override
            public long getDelay(TimeUnit unit) {
                return unit.convert(
                        this.sendTimeMs - System.currentTimeMillis(),
                        TimeUnit.MILLISECONDS);
            }

            /**
             * 在阻塞队列中的排序规则
             */
            @Override
            public int compareTo(Delayed o) {
                if (o instanceof Msg) {
                    Msg msg1 = (Msg) o;
                    return Integer.compare(this.priority, msg1.priority);
                }
                return 0;
            }
        }

        final DelayQueue<Msg> queue = new DelayQueue<>();

        new Thread(() -> {
            while (flag || !queue.isEmpty()) {
                try {
                    // 此方法会进行阻塞，直到时间到了，返回结果
                    Msg msg = queue.take();
                    System.out.printf("定时发送的时间：%s，实际发送的时间：%s，接收的消息：%s\n",
                            msg.sendTimeMs,
                            System.currentTimeMillis(),
                            msg);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        for (int i = 5; i >= 1; i--) {
            queue.put(new Msg(i, i + "", System.currentTimeMillis() + i * 2000L));
        }
        flag = false;
    }


    /**
     * 1. 一个由链表组成的无界阻塞TransferQueue队列。
     * <p>
     * 2. 相对于其他阻塞队列，多了tryTransfer和transfer方法。
     */
    private static void linkedTransferQueue() {
        final LinkedTransferQueue<String> queue = new LinkedTransferQueue<>();

        // 如果存在一个消费者已经等待接受它，则立即传送指定的元素，否则返回false，并且不进入队列，立即返回结果。
        queue.tryTransfer("");

        // 如果存在一个消费者已经等待接受它，则立即传送指定的元素，否则等待，直到元素被消费者接收。
        // transfer和SynchronousQueue的put方法类似。
        // queue.transfer("");

        // 如果至少有一位消费者在等待， 则返回true
        queue.hasWaitingConsumer();

        // 获取所有等待获取元素的消费线程数量
        queue.getWaitingConsumerCount();
    }

    public static void main(String[] args) {
    }
}
