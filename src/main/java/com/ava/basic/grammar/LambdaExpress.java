package com.ava.basic.grammar;

import lombok.ToString;

import java.util.function.*;

/**
 * @Author lancer
 * @Date 2023/3/10 21:09
 * @Description
 */
public class LambdaExpress {
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
