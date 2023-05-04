package com.ivi.grammar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * @Author lancer
 * @Date 2023/3/9 13:35
 * @Description 加载某个类所依赖的所有类的过程称为类的解析。
 * Launcher类中进行ClassLoader的初始化
 * 引导类加载器 BootstrapClassLoader是C++实现的
 * 扩展类加载器 sum.misc.Launcher$ExtClassLoader
 * 系统类加载器 sum.misc.Launcher$AppClassLoader
 * <p><p>
 * 打破双亲委派机制，需要重写loadClass方法
 */
public class ClazzLoader {
    static class MyClassLoader extends ClassLoader {

        private final String classpath;

        public MyClassLoader(String classpath) {
            this.classpath = classpath;
        }

        // 指定父加载器
        public MyClassLoader(ClassLoader parent, String classpath) {
            // 当parent为null时，则直接调用引导类加载器 - BootstrapClassLoader
            super(parent);
            this.classpath = classpath;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if (name.startsWith("com.ava.basic.grammar")) {
                return this.findClass(name);
            }
            return super.loadClass(name);
        }

        /**
         * 自定义类加载器应该覆盖findClass方法，以查找类的字节码，并通过调用defineClass方法将字节码传给虚拟机
         *
         * @param name 全限定类名
         */
        @Override
        protected Class<?> findClass(String name) {
            // String path = this.classpath + name.replace(".", File.separator) + ".class";
            // jar包的写法，还可以使用URLClassLoader
            String path = "jar:file://" + this.classpath + "!/" + name.replace(".", File.separator) + ".class";

            try (InputStream is = new URL(path).openStream();
                 ByteArrayOutputStream bao = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    bao.write(buffer, 0, len);
                }
                byte[] bytes = bao.toByteArray();

                return super.defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    /**
     * 编写自定义类加载器时，如果没有太过复杂的需求，可以直接继承URLClassLoader，可以避免自己去编写findClass()
     */
    /*static class MyURLClassLoader extends URLClassLoader {

        public MyURLClassLoader(URL[] urls) {
            super(urls);
        }
    }*/
    public static void main(String[] args) throws ClassNotFoundException {
        // 加载类的同时会初始化，自定义的ClassLoader不会
        Class.forName("com.ivi.grammar.AnnotationLearn");

        System.out.println("===============");

        Class<?> aClass = Thread.currentThread().getContextClassLoader()
                .loadClass("com.ivi.grammar.AnnotationLearn");
        try {
            Method say = aClass.getDeclaredMethod("say");
            say.setAccessible(true);
            say.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("===============");


        // 同一个类被不同的类加载器加载，则他们的Class对象是不同的
        MyClassLoader loader = new MyClassLoader(
                "/Users/lancer/IdeaProjects/God/kubernetes/test.jar");
        try {
            // AnnotationLearn的Class在AppClassLoader中存在，但是MyClassLoader中不存在，由于重写了loadClass方法，破坏了双亲委派机制，
            // 故MyClassLoader去加载时会重新去加载Object类（此时从用户提供的findClass找），抛出FileNotFoundException异常
            Class<?> c1 = loader.loadClass("com.ivi.grammar.AnnotationLearn");
            Method say = c1.getDeclaredMethod("say");
            say.setAccessible(true);
            say.invoke(null);

            loader = new MyClassLoader(
                    "/Users/lancer/IdeaProjects/God/kubernetes/test.jar");
            Class<?> c2 = loader.findClass("com.ava.grammar.AnnotationLearn");
            System.out.println(c1 == c2);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
