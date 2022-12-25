package com.ava.basic.juc.code.basic;

/**
 * AQS => AbstractQueuedSynchronizer: 用来构建锁或者其他同步器组件的重量级基础框架及整个JUC体系的基石
 * <p>
 * 通过内置的FIFO队列来完成资源获取线程的排队工作，并通过一个int类型的变量state表示持有锁的状态
 * <p>
 * ReentrantLock、CountDownLatch、Semaphore、CyclicBarrier、ReentrantReadWriteLock、SynchronousQueue
 */
public class AQS {

    //CLH队列(FIFO)：是一个单向链表，AQS中的队列是CLH变体的虚拟双向队列FIFO
    public static void main(String[] args) {
    }
}
