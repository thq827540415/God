package com.ava.basic.jvm.code.gc;

/**
 * 大对象直接进入老年代
 * -verbose:gc -Xms20m -Xmx20m -Xmn10m -XX:+PrintGCDetails -XX:SurvivorRatio=8 -XX:+UseSerialGC -XX:PretenureSizeThreshold=3145728
 * -XX:PretenureSizeThreshold只对的Serial和ParNew两款新生代收集器有效，其他的新生代收集器如Parallel Scavenge不支持，
 * 如果必须使用次参数进行调优，可考虑ParNew加CMS的收集器组合
 */
public class PretenureSizeThreshold {

    private static final int _1MB = 1024 * 1024;

    public static void main(String[] args) {
        byte[] allocation;
        allocation = new byte[4 * _1MB];
    }
}
