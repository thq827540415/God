package com.ivi.basic;

import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * introduce some basic grammar about java
 */
public class JavaBasicGrammar {

    // 访问权限修饰符
    private static class ModifySign {
        /**
         * private 只有本类中可用
         * default 只有同包中才能使用
         * protected 只有同包或者不同包子类中可以使用
         * public everywhere
         */
        @ToString
        private static class Father {
            public String name;
            int age;
            protected String gender;
            private boolean flag;

            protected void sing() {
            }

            /**
             * final修饰的方法不能被子类重写，但是能被继承
             */
            public final void jump() {
            }
        }

        /**
         * final修饰的类不能被继承
         */
        @ToString
        public static final class Son extends Father {

            /**
             * 重写的方法权限应该 >= 父类方法的权限
             */
            @Override
            public void sing() {
                System.out.println("this is son sing");
            }

            public static void main(String[] args) {
                Father s = new Son();
                s.age = 20;
                s.flag = true;
                System.out.println(s);
                s.sing();
            }
        }

        public static void main(String[] args) {
        }
    }

    // 封装
    // 通过访问权限，隐藏内部数据，外部仅能通过类提供的有限的接口访问、修改内部数据
    private static class Encapsulation {
        private static class Wallet {
            private String id;
            private long createTime;
            private BigDecimal balance;
            private long balanceLastModifiedTime;

            public Wallet() {
                this.id = UUID.randomUUID().toString();
                this.createTime = System.currentTimeMillis();
                this.balance = BigDecimal.ZERO;
                this.balanceLastModifiedTime = System.currentTimeMillis();
            }

            public String getId() {
                return this.id;
            }

            public long getCreateTime() {
                return this.createTime;
            }

            public BigDecimal getBalance() {
                return this.balance;
            }

            public long getBalanceLastModifiedTime() {
                return this.balanceLastModifiedTime;
            }

            public void increaseBalance(BigDecimal increasedAmount) {
                if (increasedAmount.compareTo(BigDecimal.ZERO) < 0) {
                    System.out.println();
                    return;
                }
                this.balance.add(increasedAmount);
                this.balanceLastModifiedTime = System.currentTimeMillis();
            }

            public void decreaseBalance(BigDecimal decreasedAmount) {
                if (decreasedAmount.compareTo(BigDecimal.ZERO) < 0) {
                    return;
                }
                if (decreasedAmount.compareTo(this.balance) > 0) {
                    return;
                }
                this.balance.subtract(decreasedAmount);
                this.balanceLastModifiedTime = System.currentTimeMillis();
            }
        }
    }

    // 多态
    private static class Polymorphism {
        @FunctionalInterface
        private interface People {
            // 默认成员变量为public static final
            public static final String ignored = "";

            // 默认成员方法为public abstract
            public abstract void sing();

            // 默认静态方法为public，静态方法不会被重写
            public static void jump() {
                System.out.println("this is people jump");
            }

            // 默认方法 -> JDK8特有
            public default void rap() {
                System.out.println("this is people rap");
            }

            public static class N {
                private String name;
            }
        }

        private abstract static class Animal {
            abstract void sing();

            private static final String hello = "";

            protected void rap() {
            }
        }

        private static class Father implements People {
            public Father() {
                System.out.println("Construct father is invoked");
            }

            @Override
            public void sing() {
                System.out.println("this is father sing");
            }

            public void father() {
            }
        }

        private static class Son extends Father {
            public Son() {
                System.out.println("Construct son is invoked");
            }

            @Override
            public void sing() {
                System.out.println("this is son sing");
            }

            public void son() {
            }
        }

        public static void main(String[] args) {
            People p1 = () -> System.out.println("this is interface People");
            p1.rap();
            p1.sing();
            System.out.println("====================");

            People p2 = new Father();
            p2.rap();
            p2.sing();
            System.out.println("====================");

            People p3 = new Son();
            p3.rap();
            p3.sing();
            System.out.println("====================");


            Father f1 = new Father();
            f1.sing();
            System.out.println("====================");

            Father f2 = new Son();
            f2.sing();
        }
    }

    // 泛型
    private static class Generic {
        // 泛型类、泛型接口、泛型方法
        // 通配泛型 -> ? 、 ? extends Object、 ? supper T
        // 由于存在泛型擦除 -> E o = new E()、E[] elements = new E[capacity]都是不可取的
        // E[] elements = (E[])new Object[capacity]可以

        // 使用泛型类创建泛型数组也是不允许的


        // List<Number>并不能接收List<Integer>或List<Double>类型的参数
        // List<? extends Number>则可以接收

        private static void test(List<Number> list) {
        }

        private static void test1(List<? extends Number> list) {
        }

        /**
         * 与demo1等价
         */
        private static <T> void demo(List<? extends T> l1, List<T> l2) {
        }

        private static <T> void demo1(List<T> l1, List<? super T> l2) {
        }

        private static <T extends Comparable<T>> void compare(T v1, T v2) {
        }


        public static void main(String[] args) {
            // List<Integer> list = Arrays.asList(1, 2, 3, 4);
        }
    }
}
