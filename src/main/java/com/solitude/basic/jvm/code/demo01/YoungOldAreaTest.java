package com.solitude.basic.jvm.code.demo01;

/**
 * @Author lancer
 * @Date 2022/4/6 00:23
 * @Description -Xms60m -Xmx60m -XX:NewRatio=2 -XX:SurvivorRatio=8 -XX:+PrintGCDetails
 */
public class YoungOldAreaTest {
    public static void main(String[] args) {
        // 20m
        byte[] buffer = new byte[1024 * 1024 * 20];
    }
}
