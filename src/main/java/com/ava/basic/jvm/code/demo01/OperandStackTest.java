package com.ava.basic.jvm.code.demo01;

/**
 * @Author lancer
 * @Date 2022/4/4 21:45
 * @Description
 */
public class OperandStackTest {
    public void testAddOperation() {
        // byte、short、char、boolean都以int型来保存
        byte i = 15;
        short j = 8;
        int k = i + j;

        long m = 12L;
        int n = 800;
        // 存在宽化类型转换，将n编程long类型一起放入栈中
        m = m * n;
    }
}
