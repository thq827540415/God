package com.solitude.basic.juc;

/**
 * 内部使用SkipList实现的，放入的元素会进行排序，排序算法支持两种方式来指定（必选1种）：
 *  1. 通过构造方法传入一个Comparator
 *  2. 放入的元素实现Comparable接口
 *  如果两种都指定了，按照第一种排序规则
 * 特性：
 *  1. 迭代结果和存入顺序不一致
 *  2. 放入的元素会排序
 *  3. key和value都不能为空
 *  4. 线程安全的
 * 可用于实现ConcurrentSkipListSet
 */
public class MyConcurrentSkipListMap {
    public static void main(String[] args) {

    }
}
