package com.ivi.jvm.code.demo01;


/**
 * @Author lancer
 * @Date 2022/3/31 10:50 下午
 * @Description
 */
public class ByteCodeInterview01 {

    /**
     * i++和++i有什么区别
     */
    public void test01() {
        int i = 10;
        ++i;
        System.out.println(i);
    }


    public void test02() {
        int i = 10;
        i = i++;
        // 10
        System.out.println(i);
    }

    public void test03() {
        int i = 2;
        i *= i++;
        // 4
        System.out.println(i);
    }

    public void test04() {
        int k = 10;
        k = k + (k++) + (++k);
        // 32
        System.out.println(k);
    }

    public void test05() {
        Boolean aTrue = Boolean.TRUE;
        Boolean b1 = true;
        boolean b2 = true;
        // true
        System.out.println(b1 == b2);
        // true
        System.out.println(aTrue == b1);
    }

    /**
     * String声明的字面量数据都放在字符串常量池中
     * jdk6中字符串常量池存放在方法区（永久代）
     * jdk7及以后，字符串常量池存放在堆空间
     */
    public void test06() {
        /*
         * jdk7及以后
         * str指向堆空间中new出来的那个helloworld，
         *  1. 调用str.intern()方法，会在常量池中生成一个指向堆空间helloworld的地址，然后str1发现常量池中存在helloworld的地址，则一同指向堆空间那个helloworld
         *  2. 不调用str.intern()方法，则str1发现常量池中不存在helloworld及helloworld的地址，则重新生成一个
         *
         * jdk6
         * str指向堆空间中new出来的那个helloworld，
         *  1. 调用str.intern()方法，会在常量池中生成一个helloworld，然后str1就指向常量池的那个helloworld
         *
         *
         * */
        String str = new String("hello") + new String("world");
        System.out.println(str.hashCode());
        str.intern();
        String str1 = "helloworld";
        System.out.println(str.hashCode());
        String str2 = "hello" + "world";
        // false --> true(加上intern(),在str1声明之前)
        System.out.println(str == str1);
        System.out.println(str == str2);
    }
}
