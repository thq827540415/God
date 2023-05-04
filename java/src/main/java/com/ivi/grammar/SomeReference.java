package com.ivi.grammar;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class SomeReference {

    private static class M {
        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            System.out.println("do the finalize");
        }
    }

    static void strongRef() throws InterruptedException {
        M m = new M();

        m = null;
        System.gc();

        TimeUnit.SECONDS.sleep(1);
    }

    // -Xms20m -Xmx20m
    static void softRef() throws InterruptedException {
        SoftReference<byte[]> sr = new SoftReference<>(new byte[12 * 1024 * 1024]);
        System.out.println("null: " + Objects.isNull(sr.get()));
        System.gc();
        System.out.println("after gc, null: " + Objects.isNull(sr.get()));

        TimeUnit.SECONDS.sleep(1);
        // 当内存不足时，才回收弱引用
        byte[] other = new byte[10 * 1024 * 1024];
        System.out.println("null: " + Objects.isNull(sr.get()));
    }

    static void weakRef() {
        WeakReference<M> wr = new WeakReference<>(new M());
    }

    static void phantomRef() {
        ReferenceQueue<M> queue = new ReferenceQueue<>();
        PhantomReference<M> pr = new PhantomReference<>(new M(), queue);
    }

    public static void main(String[] args) throws InterruptedException {
    }
}
