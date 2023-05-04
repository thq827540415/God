package com.ivi.jvm.code.demo01;

/**
 * @Author lancer
 * @Date 2022/4/5 00:49
 * @Description
 */
public class HeapSpaceInitial {
    public static void main(String[] args) {
        // 返回JVM中的堆内存总量
        long initialMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        // 返回JVM试图使用的最大堆内存量
        long maxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024;

        System.out.println("-Xms : " + initialMemory + "MB");
        System.out.println("-Xmx : " + maxMemory + "MB");

        System.out.println("系统内存大小约为：" + initialMemory * 64.0 / 1024 + "GB");
        System.out.println("系统内存大小约为：" + maxMemory * 4.0 / 1024 + "GB");
    }
}
