package com.ava.basic.grammar;

import java.lang.annotation.*;
import java.util.Arrays;

/**
 * @Author lancer
 * @Date 2023/3/8 08:46
 * @Description
 */
public class AnnotationLearn {

    static {
        System.out.println("Hello AnnotationLearn");
    }

    static void say() {
        System.out.println("Hello");
    }

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
