package com.ivi.jvm.code.demo01;

/**
 * @Author lancer
 * @Date 2022/4/4 20:35
 * @Description
 */
public class LocalVariablesThreadSafeTest {
    public static void main(String[] args) {

    }

    /**
     * 线程安全的，线程内创建StringBuilder
     */
    public void test01() {
        StringBuilder s1 = new StringBuilder();
        s1.append("a");
        s1.append("b");
    }

    /**
     * 线程安全
     */
    public String test02() {
        StringBuilder sb = new StringBuilder();
        sb.append("a");
        sb.append("b");
        return sb.toString();
    }

    /**
     * 线程不安全, StringBuilder从外部传过来
     */
    public void test03(StringBuilder sb) {
        sb.append("a");
        sb.append("b");
    }

    public StringBuilder test04() {
        StringBuilder sb = new StringBuilder();
        sb.append("a");
        sb.append("b");
        return sb;
    }
}
