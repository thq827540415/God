package com.ava.basic.grammar;

import java.lang.reflect.*;
import java.util.Arrays;

/**
 * @Author lancer
 * @Date 2023/3/7 22:06
 * @Description 另一种生成java对象的方法 -> 运行时动态操作对象
 * <p>
 * Java运行时系统会为所有对象委会一个运行时类型标识，这个信息会跟踪每个对象所属的类。
 * 使用一个特殊的Java类 -> Class类，访问每个对象所属的类
 * Class类实际上是一个泛型类
 * <p><p>
 * 应该是相同维度和类型的数组共用一个Class
 * Every array also belongs to a class that is reflected as a Class object
 * that is shared by all arrays with the same element type and number of dimensions.
 * <p><p>
 * java的8大基本类型和void关键字
 * The primitive Java types and the keyword void are also represented as Class objects
 * <p><p>
 * 枚举属于类，注解属于接口
 * An enum is a kind of class and an annotation is a kind of interface.
 * <p><p>
 * 通过类加载器的defineClass来生成Class对象
 * Class objects are constructed automatically by the JVM as classes are loaded
 * and by calls to the defineClass method in the class loader.
 */
public class Reflective {
    static {
        System.out.println("Reflect");
    }

    /**
     * 获取Class对象的3种方式
     */
    void getClassType() {
        // 1. 类.class
        Class<Reflective> aClazz = Reflective.class;

        // 2. 对象.getClass()
        Class<? extends Reflective> bClazz = new Reflective().getClass();

        try {
            // 3. Class.forName(String name);
            Class<?> cClazz = Class.forName("com.ava.basic.grammar.Reflective");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Inner {
        String name = "666";

        static int age = 50;

        private final String gender = "male";

        void say() {
            System.out.println("Hello Say! " + this);
        }

        static void sing() {
            System.out.println("Hello Sing!");
        }

        private void dance() {
            System.out.println("Hello Dance!");
        }

        public Inner() {
        }

        // 反射调用
        public Inner(String msg) {
            System.out.println(msg);
        }
    }


    /**
     * 可以获取类的Fields、Methods、Constructors、Annotations、方法异常、返回值等。获取方法具体分类如下：
     * <p>1. 不带Declared，可以获取本类及所有父类中的public成员
     * <p>2. 带Declared，可以获取本类中所有成员
     * <p>3. 方法不加s，传入对应的参数类型，获取对应的成员
     */
    void clazzMethod() {
        try {
            // todo 1. Methods中的invoke方法
            Inner inner = new Inner();
            Method say = inner.getClass().getDeclaredMethod("say");
            say.invoke(inner);


            Method sing = Inner.class.getDeclaredMethod("sing");
            // 如果是静态方法，则可以直接传入null
            sing.invoke(null);


            try {
                Method dance = inner.getClass().getDeclaredMethod("dance");
                // 访问非public成员时
                dance.setAccessible(true);
                // 如果是成员方法，则必须指定为哪个对象的成员方法
                dance.invoke(null);
            } catch (NullPointerException e) {
                System.out.println("Exception: " + e);
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        System.out.println("====================================");

        try {
            // todo 2. Fields
            Inner inner = new Inner();
            Object o1 = inner.getClass().getDeclaredField("name").get(inner);
            System.out.println(o1);


            Object o2 = Inner.class.getDeclaredField("age").get(null);
            System.out.println(o2);


            try {
                Field age = inner.getClass().getDeclaredField("gender");
                age.setAccessible(true);
                Object o3 = age.get(null);
                System.out.println(o3);
            } catch (NullPointerException e) {
                System.out.println("Exception: " + e);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        System.out.println("====================================");

        try {
            // todo 3. Constructors
            Constructor<Inner> c1 = Inner.class.getDeclaredConstructor(String.class);
            c1.newInstance("This is constructor with one parameter");
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    void modifier() {
        System.out.println(Modifier.isVolatile(Reflective.class.getModifiers()));
    }




    public static void main(String[] args) {
        Class<Integer> intClazz = int.class;
        Class<Integer> integerClazz = Integer.class;
        Class<Void> voidClazz = void.class;

        // System.out.println(double[].class.getName());
        // System.out.println(Integer[].class.getName());
        // System.out.println(Reflective[].class.getName());

        // System.out.println(Arrays.toString(Reflective.class.getDeclaredMethods()));
        new Reflective().clazzMethod();
    }
}