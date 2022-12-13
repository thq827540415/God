package com.ava.basic.juc;


import com.ava.util.CommonUtils;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * juc中的大部分类都是依赖于Unsafe来实现的，如CAS、线程挂起、线程恢复等功能。
 * 用于直接访问系统内存资源、自主管理内存资源等。
 */
public class E87_Unsafe {
    private static Unsafe unsafe;

    static {
        try {
            // 使用反射或取Unsafe单例对象
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            // 由于是静态的，传入null
            unsafe = (Unsafe) field.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ========================================= CAS =========================================
    /**
     * CAS是基于Unsafe类的。
     * <p>
     * CAS包含三个操作数：内存位置、预期原值及新值。
     * CAS是一条CPU的原子指令（cmpxchg指令），执行该指令时，如果是多核系统，就会给总线加锁，成功加锁的线程开始执行。
     * CAS的原子性实际上是由CPU实现的。
     * CAS在j.u.c.atomic相关类，Java AQS、JUC并发集合等实现上有广泛应用。
     */
    private static class Static {
        private static int count;

        private Object o;

        static {
            System.out.println("Static.class init...");
        }
    }

    /**
     * 如果任务逻辑比较耗时，则CAS性能比synchronized好，反之亦然。
     */
    private static void synchronizedCompareToMutilCAS() {
        class MethodInner {
            volatile int cnt1 = 0;
            volatile int cnt2 = 0;
            volatile int cnt3 = 0;

            /**
             * 手动模拟CAS
             */
            synchronized boolean compareAndSwapInt(int expectCnt, int newCnt) {
                if (cnt2 == expectCnt) {
                    cnt2 = newCnt;
                    return true;
                }
                return false;
            }
        }
        MethodInner o = new MethodInner();

        // 1. use synchronized
        try {
            long start1 = System.currentTimeMillis();
            int threadNums1 = 100;
            CountDownLatch cdl1 = new CountDownLatch(threadNums1);
            runTaskWithFixedThread(
                    threadNums1,
                    () -> {
                        for (int i = 0; i < 10; i++) {
                            synchronized (Static.class) {
                                CommonUtils.sleep(5, TimeUnit.MILLISECONDS);
                                o.cnt1++;
                            }
                        }
                        cdl1.countDown();
                    });
            cdl1.await();
            System.out.println("synchronized耗时：" +
                    (System.currentTimeMillis() - start1) + ", count = " + o.cnt1);


            // 2. use simulatorCAS，手动实现一个CAS
            long start2 = System.currentTimeMillis();
            int threadNums2 = 100;
            CountDownLatch cdl2 = new CountDownLatch(threadNums2);
            runTaskWithFixedThread(
                    threadNums2,
                    () -> {
                        for (int i = 0; i < 10; i++) {
                            CommonUtils.sleep(5, TimeUnit.MILLISECONDS);
                            int expectCnt;
                            do {
                                expectCnt = o.cnt2;
                            } while (!o.compareAndSwapInt(expectCnt, expectCnt + 1));
                        }
                        cdl2.countDown();
                    });
            cdl2.await();
            System.out.println("simulatorCAS耗时：" +
                    (System.currentTimeMillis() - start2) + ", count = " + o.cnt2);


            // 3. use Unsafe类中的compareAndSwapInt
            // 通过unsafe.objectFieldOffset获取对象中成员字段的偏移量，与对象搭配使用Unsafe类
            long cnt3Offset = unsafe.objectFieldOffset(MethodInner.class.getDeclaredField("cnt3"));
            long start3 = System.currentTimeMillis();
            int threadNums3 = 100;
            CountDownLatch cdl3 = new CountDownLatch(threadNums3);
            runTaskWithFixedThread(
                    threadNums3,
                    () -> {
                        for (int i = 0; i < 10; i++) {
                            CommonUtils.sleep(5, TimeUnit.MILLISECONDS);
                            int expectCnt;
                            do {
                                expectCnt = o.cnt3;
                            } while (!unsafe.compareAndSwapInt(o, cnt3Offset, expectCnt, expectCnt + 1));
                        }
                        cdl3.countDown();
                    });
            cdl3.await();
            System.out.println("Unsafe#compareAndSwapInt耗时：" +
                    (System.currentTimeMillis() - start3) + ", count = " + o.cnt3);


            // 4. use AtomicInteger，原子类底层使用的是Unsafe类提供的CAS操作。
            // set()和get()不是原子操作，但是由于value的volatile性，多线程环境下均可见。
            final AtomicInteger cnt = new AtomicInteger();
            long start4 = System.currentTimeMillis();
            int threadNums4 = 100;
            CountDownLatch cdl4 = new CountDownLatch(threadNums4);
            runTaskWithFixedThread(
                    threadNums4,
                    () -> {
                        for (int j = 0; j < 10; j++) {
                            CommonUtils.sleep(5, TimeUnit.MILLISECONDS);
                            cnt.incrementAndGet();
                            // cnt.getAndIncrement();
                        }
                        cdl4.countDown();
                    });
            cdl4.await();
            System.out.println("AtomicInteger耗时：" +
                    (System.currentTimeMillis() - start4) + ", count = " + cnt);


            // 5. use Unsafe中的原子方法 -> 模拟AtomicInteger
            // 通过unsafe.staticFieldOffset获取类中静态字段的偏移量，，与类搭配使用Unsafe类
            long countOffset = unsafe.staticFieldOffset(Static.class.getDeclaredField("count"));
            long start5 = System.currentTimeMillis();
            int threadNums5 = 100;
            CountDownLatch cdl5 = new CountDownLatch(threadNums5);
            runTaskWithFixedThread(
                    threadNums5,
                    () -> {
                        for (int j = 0; j < 10; j++) {
                            CommonUtils.sleep(5, TimeUnit.MILLISECONDS);
                            unsafe.getAndAddInt(Static.class, countOffset, 1);
                        }
                        cdl5.countDown();
                    });
            cdl5.await();
            System.out.println("Unsafe#getAndAddInt耗时：" +
                    (System.currentTimeMillis() - start5) + ", count = " + Static.count);
        } catch (InterruptedException | NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }


    private static void parkAndUnPark() {
        // 1. 每个线程有个permit，默认为0，调用park的时候，发现是0会阻塞当前线程。
        //  调用unpark之后，许可会被置为1，并唤醒该线程，park被唤醒后，许可又会置为0。多次调用unpark效果一致。
        Thread t1 = new Thread(() -> {
            System.out.println(System.currentTimeMillis() + ", " + Thread.currentThread().getName() + ", start");
            // 阻塞线程，第一个参数表示time是绝对时间还是相对时间，时间单位纳秒
            unsafe.park(false, 0);
            System.out.println(System.currentTimeMillis() + ", " + Thread.currentThread().getName() + ", end");
        }, "thread1");
        t1.start();

        CommonUtils.sleep(1, TimeUnit.SECONDS);
        // 取消阻塞线程
        unsafe.unpark(t1);


        // 2. 如果在park之前调用了unpark方法，执行park方法的时候，不会阻塞。
        Thread t2 = new Thread(() -> {
            System.out.println(System.currentTimeMillis() + ", " + Thread.currentThread().getName() + ", start");
            unsafe.unpark(Thread.currentThread());
            unsafe.park(false, TimeUnit.SECONDS.toNanos(3));
            System.out.println(System.currentTimeMillis() + ", " + Thread.currentThread().getName() + ", end");
        }, "thread2");
        t2.start();

        CommonUtils.sleep(1, TimeUnit.SECONDS);

        // 3. 当park第二个参数 not in (0, 1)时，表示超时阻塞。
        Thread t3 = new Thread(() -> {
            System.out.println(System.currentTimeMillis() + ", " + Thread.currentThread().getName() + ", start");
            unsafe.park(false, TimeUnit.SECONDS.toNanos(1));
            unsafe.park(false, TimeUnit.SECONDS.toNanos(1));
            System.out.println(System.currentTimeMillis() + ", " + Thread.currentThread().getName() + ", end");
        }, "thread3");
        t3.start();
    }


    private static void clazz() {
        try {
            Field staticField = Static.class.getDeclaredField("count");
            // 获取给定静态字段的内存地址偏移量，这个值对于给定的字段是唯一且固定不变的。Field只能通过反射获得。
            System.out.println(unsafe.staticFieldOffset(staticField));
            System.out.println(unsafe.objectFieldOffset(Static.class.getDeclaredField("o")));
            // 获取一个静态类中给定字段的对象指针
            System.out.println(unsafe.staticFieldBase(staticField) == Static.class);

            // 判断是否初始化一个类，通常在获取一个类的静态属性时
            System.out.println(unsafe.shouldBeInitialized(Static.class));
            if (unsafe.shouldBeInitialized(Static.class)) {
                System.out.println("Static 需要进行初始化");
                // 检测给定的类是否已经初始化。
                unsafe.ensureClassInitialized(Static.class);
            }
            System.out.println(unsafe.shouldBeInitialized(Static.class));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建固定线程执行单个任务
     */
    private static void runTaskWithFixedThread(int threadNums, Runnable task) {
        ExecutorService threadPool = Executors.newFixedThreadPool(threadNums);
        for (int i = 0; i < threadNums; i++) {
            threadPool.execute(task);
        }
        threadPool.shutdown();
    }

    public static void main(String[] args) throws NoSuchFieldException, IllegalAccessException {
        // 1. CAS操作相关的方法
        // offset -> 某个字段的地址偏移量
        // theUnsafe.compareAndSwapInt(Object o, long offset, int expected, int update);
        // theUnsafe.compareAndSwapLong(Object o, long offset, long expected, long update);
        // theUnsafe.compareAndSwapObject(Object o, long offset, Object expected, Object update);
        synchronizedCompareToMutilCAS();


        // 2. 保证变量的可见性，Unsafe提供了和volatile关键字一样的功能的方法。
        // put后立马更新到主内存。8中基本类型的putXxxVolatile和一种putObjectVolatile
        // theUnsafe.putIntVolatile(Object o, long offset, int i);
        // 每次get都从主内存中获取。8中基本类型的getXxxVolatile和一种getObjectVolatile
        // theUnsafe.getIntVolatile(Object o, long offset);
        // 有序、延迟版本的putObjectVolatile方法，只有在field被volatile修饰的时候，多线程可见最新值
        // 否则，可能会在一小段时间内，还是可以读到旧的值
        // theUnsafe.putOrderedObject(Object o, long l, Object o1);
        // theUnsafe.putOrderedInt(Object o, long l, int i);
        // theUnsafe.putOrderedLong(Object o, long l, long l1);


        // 3. 原子操作相关的方法，底层使用CAS + getXxxVolatile()/putXxxVolatile()
        // 在数值基础上增加值
        // theUnsafe.getAndAddInt(Object o, long l, int i);
        // theUnsafe.getAndAddLong(Object o, long l, long l1);
        // 在数值基础上修改值
        // theUnsafe.getAndSetInt(Object o, long l, int i);
        // theUnsafe.getAndSetLong(Object o, long l, long l1);
        // theUnsafe.getAndSetObject(Object o, long l, Object o1);


        // 4. 线程调度相关的方法
        // parkAndUnPark();


        // 5. 与Class相关的方法
        // clazz();


        // 6. 与对象有关的方法
        // 获取指定对象中某字段的值
        // Object o = theUnsafe.getObject(Object o, long l)
        // 给定对象的指定地址偏移量设值
        // theUnsafe.putObject(Object o, long l, Object o1);


        // 7. 与数组相关的方法。两者配合起来使用，即可定位数组中每个元素在内存中的位置
        // 返回数组中第一个元素的偏移地址
        // int offset = theUnsafe.arrayBaseOffset(Class<?> arrayClass);
        // 返回数组中一个元素的占用的大小
        // int offset = theUnsafe.arrayIndexScale(Class<?> arrayClass);


        // 8. 与内存屏障相关操作
        // 禁止load操作重排序 -> 屏障前的load操作不能被重排序到屏障后，反之亦然
        // theUnsafe.loadFence();
        // 禁止store操作重排序 -> 屏障前的load操作不能被重排序到屏障后，反之亦然
        // theUnsafe.storeFence();
        // 进行load、store操作重排序
        // theUnsafe.fullFence();
    }
}
