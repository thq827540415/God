package com.ava.basic.jvm.code.demo01;

/**
 * @Author lancer
 * @Date 2022/4/6 20:34
 * @Description
 */
public class FinalizeObj {
    // 静态变量，属于GC Root
    public static FinalizeObj obj;

    /**
     * GC在回收对象前调用该方法
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        System.out.println("调用当前类重写finalize方法");
        obj = this;
    }

    public static void main(String[] args) {
        try {
            obj = new FinalizeObj();
            obj = null;
            System.gc();
            System.out.println("第一次gc");
            // 因为Finalizer线程优先级很低
            Thread.sleep(2000);
            if (obj == null) {
                System.out.println("obj is dead");
            } else {
                System.out.println("obj is alive");
            }

            obj = null;
            System.out.println("第二次gc");
            Thread.sleep(2000);
            if (obj == null) {
                System.out.println("obj is dead");
            } else {
                System.out.println("obj is alive");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
