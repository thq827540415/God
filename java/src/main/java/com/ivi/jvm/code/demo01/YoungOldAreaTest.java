package com.ivi.jvm.code.demo01;

/**
 * @Author lancer
 * @Date 2022/4/6 00:23
 * @Description -Xms60m -Xmx60m -XX:NewRatio=2 -XX:SurvivorRatio=8 -XX:+PrintGCDetails
 */
public class YoungOldAreaTest {
    public static void main(String[] args) {
        // YoungGen : OldGen = 2 -> YoungGen = 60 * 1 / 3 = 20m, OldGen = 60 - 20 = 40m
        // 往堆中分2次放入共20m
        byte[] buffer1 = new byte[1024 * 1024 * 10];
        // YoungGen内存不够了，只能直接放入OldGen中
        byte[] buffer2 = new byte[1024 * 1024 * 10];
        // System.gc()尽量不用于生产环境
        // System.gc();
    }
}
