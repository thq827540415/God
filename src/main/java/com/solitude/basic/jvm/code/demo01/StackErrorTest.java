package com.solitude.basic.jvm.code.demo01;

/**
 * @Author lancer
 * @Date 2022/4/4 10:57
 * @Description
 */
public class StackErrorTest {
    private static int count = 1;

    public static void main(String[] args) {

        test02();

    }

    // StackOverFlow
    public static void test01() {
        try {
            Thread.sleep(100000);
            count++;
            test01();
        } catch (Throwable e) {
            System.out.println(count);
        }
    }

    // OOM
    public static void test02() {
        while (true) {
            new Thread(() -> {
                while (true) {
                }
            }).start();
        }
    }
}
