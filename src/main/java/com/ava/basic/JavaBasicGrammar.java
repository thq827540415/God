package com.ava.basic;

import lombok.ToString;

import java.lang.annotation.*;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.*;

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
                if  (decreasedAmount.compareTo(BigDecimal.ZERO) < 0) {
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

    // 反射
    private static class Reflection {
        private static class Father {
            private String name;
            public int age;

            public Father() {
            }

            public Father(int age) {
                this.age = age;
            }

            public void rap() {
            }
        }

        private static class Son extends Father {
            public String name;
            private int age;

            private Son() {
            }

            public Son(String name) {
                this.name = name;
            }

            public Son(String name, int age) {
                this.name = name;
                this.age = age;
            }

            private void sing() {
            }

            public void jump() {
            }
        }

        public static void main(String[] args) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
            Class<Son> sonClazz = Son.class;
            // 获取包括父类的所有public字段
            System.out.println(Arrays.toString(sonClazz.getFields()));
            // 获取不包括父类的所有字段
            System.out.println(Arrays.toString(sonClazz.getDeclaredFields()));

            System.out.println("=============================");

            // 都只能获取自身的构造方法
            System.out.println(Arrays.toString(sonClazz.getConstructors()));
            System.out.println(Arrays.toString(sonClazz.getDeclaredConstructors()));

            System.out.println("=============================");

            // 获取包括父类、Object类在内的所有public方法
            System.out.println(Arrays.toString(sonClazz.getMethods()));
            // 获取自身的所有方法
            System.out.println(Arrays.toString(sonClazz.getDeclaredMethods()));

            System.out.println("vvvvvvvvvvvvvvvvvvvvvvvvvvvv");

            Class<Father> fatherClazz = Father.class;
            System.out.println(Arrays.toString(fatherClazz.getFields()));
            System.out.println(Arrays.toString(fatherClazz.getDeclaredFields()));

        }
    }

    // 注解
    private static class Annotation {
        @Retention(value = RetentionPolicy.RUNTIME)
        // 类或接口：TYPE、形参：PARAMETER、构造方法：CONSTRUCTOR、局部变量：LOCAL_VARIABLE、
        @Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
        @Documented
        // 表示子类也可获得该注解效果, @Target必须能修饰Class
        @Inherited
        private @interface MyAnnotation {
            // ()是特殊语法
            public String name();

            int age() default 18;

            String[] hobby();
        }

        @MyAnnotation(name = "zs", hobby = {"no", "drink"})
        private static class Father {
        }

        private static class Son extends Father {
        }

        public static void main(String[] args) {
            // 最后通过反射获取RetentionPolicy为RUNTIME注解
            Class<Son> sonClazz = Son.class;
            System.out.println(Arrays.toString(sonClazz.getAnnotations()));
            System.out.println(Arrays.toString(sonClazz.getDeclaredAnnotations()));

            MyAnnotation annotation = sonClazz.getAnnotation(MyAnnotation.class);
            System.out.println(annotation.annotationType());


        }
    }

    // Lambda表达式
    private static class Lambda {

        @ToString
        private static class Father {
            String name;
            int age;

            Father() {
                this.name = "supplier";
                this.age = 0;
            }

            Father(String name, int age) {
                this.name = name;
                this.age = age;
            }

            private static void sing() {
                System.out.println("sing is invoked");
            }

            private void rap() {
                System.out.println("rap is invoked");
            }
        }

        /**
         * 自定义函数式接口
         */
        @FunctionalInterface
        private interface MyFunctionalInterface<T1, T2, T3, T4> {
            T4 compute(T1 t1, T2 t2, T3 t3);
        }

        private static void demo(
                Supplier<Father> supplier,
                BiFunction<String, Integer, Father> biFunction) {
            System.out.println("supplier father is " + supplier.get());
            System.out.println("biFunction father is " + biFunction.apply("1", 12));
        }

        /**
         * JDK8自带的函数式接口大部分都在java.util.function中
         */
        public static void main(String[] args) {
            // void run()
            Runnable runnable = () -> System.out.println("hello");
            // T get()
            Supplier<String> supplier = () -> "123";
            // void accept(T t)
            Consumer<Integer> consumer = System.out::println;
            // R apply(T t)
            Function<Integer, Integer> function = a -> a + 1;
            // boolean test(T t)
            Predicate<Integer> predicate = a -> a > 10;

            // void accept(T t, U u)
            BiConsumer<Integer, Integer> biConsumer = (a, b) -> System.out.println(a + b);
            // R apply(T t, U u)
            BiFunction<Integer, Integer, Integer> biFunction = Integer::sum;
            // boolean test(T t, U u)
            BiPredicate<Integer, Integer> biPredicate = (a, b) -> a > b;

            // 调用构造方法 ->
            demo(Father::new, Father::new);

            // 静态方法用类调用
            Runnable r = Father::sing;
            r.run();

            System.out.println("==================");

            // 非静态方法用对象调用
            Father f = new Father();
            Runnable r2 = f::rap;
            r2.run();

            MyFunctionalInterface<Integer, Integer, Integer, Integer> myFunctionalInterface =
                    (a, b, c) -> a + b + c;
        }
    }

    // 网络IO
    private static class Net {
    }
}
