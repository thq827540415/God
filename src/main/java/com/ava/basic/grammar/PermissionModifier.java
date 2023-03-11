package com.ava.basic.grammar;

/**
 * @Author lancer
 * @Date 2023/3/7 21:09
 * @Description 默认修饰符（package-private）与protected的区别
 */
public class PermissionModifier {

    // 只有同包或子类能访问
    protected boolean isProtected = true;

    // 只有同包能访问
    boolean isDefault = true;

    public static void main(String[] args) {
        /*
         * 默认的只有同包下能访问，protected不同包的子类也可以访问
         */
    }
}
