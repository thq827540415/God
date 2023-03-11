package com.ava.basic.juc.code;

public class ThreadLocalTest {

    /**
     * 每个线程对应一个ThreadLocal
     */
    static final ThreadLocal<Integer> THREAD_LOCAL_NUM = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };

    public static void main(String[] args) {
       for (int i = 0; i < 3; i++) {
           new Thread(() -> {
               for (int j = 0; j < 3; j++) {
                   Integer n = THREAD_LOCAL_NUM.get();
                   n += 1;
                   THREAD_LOCAL_NUM.set(n);
                   System.out.println(Thread.currentThread().getName() + ": ThreadLocal num = " + n);
               }
           }).start();
       }
    }
}
