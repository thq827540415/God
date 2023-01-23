package com.ava.basic.jvm.code.gc;

/**
 * gc日志输出
 * -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=D:\
 */
public class TenuringThreshold {

    private static final int _1MB = 1024 * 1024;

    /**
     * 长期存活对象将进入老年代
     * -Xms20m -Xmx20m -Xmn10m -XX:+PrintGCDetails -XX:SurvivorRatio=8 -XX:+UseSerialGC -XX:MaxTenuringThreshold=1 -XX:+PrintTenuringDistribution
     */
    private static void test01() {
        // 256KB
        byte[] allocation1 = new byte[_1MB / 4];

        byte[] allocation2 = new byte[4 * _1MB];
        byte[] allocation3 = new byte[4 * _1MB];
        allocation3 = null;
        allocation3 = new byte[4 * _1MB];
    }


    /**
     * 动态对象年龄判断
     * -Xms20m -Xmx20m -Xmn10m -XX:+PrintGCDetails -XX:SurvivorRatio=8 -XX:+UseSerialGC -XX:MaxTenuringThreshold=15 -XX:+PrintTenuringDistribution
     */
    private static void test02() {
        // allocation1 + allocation2大于Survivor空间一半
        byte[] allocation1 = new byte[_1MB / 4];
        byte[] allocation2 = new byte[_1MB / 4];
        byte[] allocation3 = new byte[4 * _1MB];
        byte[] allocation4 = new byte[4 * _1MB];
        allocation4 = null;
        allocation4 = new byte[4 * _1MB];
    }

    public static void main(String[] args) {

    }
}
