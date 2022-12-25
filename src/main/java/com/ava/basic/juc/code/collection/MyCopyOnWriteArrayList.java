package com.ava.basic.juc.code.collection;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Collections静态类的synchronizedList的性能不高
 * CopyOnWriteArrayList内部使用数据存储数据，只有在多线程同时写操作过程中会导致其他线程阻塞。
 * 原理：
 *  在写入操作的时候，并不修改原有内容，而是在原有存放数据的数组上产生一个副本，
 * 在副本上修改数据，修改完毕之后，用副本替换原来的数组。
 * 可用于实现CopyOnWriteArraySet
 */
public class MyCopyOnWriteArrayList {
    public static void main(String[] args) throws InterruptedException {
        // 特性
        // 1. 迭代结果和存入顺序一致
        // 2. 元素不重复
        // 3. 元素可以为空
        // 4. 线程安全的
        // 5. 无界的
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();

        list.add("1");
        list.add("2");
        list.add("3");

        // 迭代器的弱一致性
        Iterator<String> iter = list.iterator();

        new Thread(() -> {
            // 会在copy出来的那个数组上操作
            list.remove(0);
            System.out.println(list);
        }).start();

        Thread.sleep(1000);
        while (iter.hasNext()) {
            // 获取的是原数组
            System.out.println(iter.next());
        }
    }
}
