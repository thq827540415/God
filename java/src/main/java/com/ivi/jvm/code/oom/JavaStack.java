package com.ivi.jvm.code.oom;

/**
 * -Xss128k
 */
public class JavaStack {
    private int stackLength = 1;

    // StackOverFlow
    private void test01() {
        stackLength++;
        test01();
    }

    // OOM
    public void test02() {
        while (true) {
            new Thread(() -> {
                while (true) {
                }
            }).start();
        }
    }


    public static void main(String[] args) {
        JavaStack oom = new JavaStack();
        try {
            oom.test02();
        } catch (Throwable e) {
            System.out.println("stack length: " + oom.stackLength);
            throw e;
        }
    }
}
