package com.ava.basic.jvm.code.gc;

/**
 * 代码在Linux环境下执行更好
 * 采用串行垃圾回收器
 * -verbose:gc -Xms20m -Xmx20m -Xmn10m -XX:+PrintGCDetails -XX:SurvivorRatio=8 -XX:+PrintCommandLineFlags -XX:+UseSerialGC
 */
public class MinorGC {
    private static final int _1MB = 1024 * 1024;

    public static void main(String[] args) {
        // 放入正常
        byte[] allocation1 = new byte[2 * _1MB];
        byte[] allocation2 = new byte[2 * _1MB];
        byte[] allocation3 = new byte[2 * _1MB];
        // 第一次Minor GC
        byte[] allocation4 = new byte[4 * _1MB];
    }
}
