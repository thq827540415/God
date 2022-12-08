package com.solitude.basic.jvm.code.demo01;

/**
 * @Author lancer
 * @Date 2022/4/4 20:35
 * @Description
 */
public class LocalVariablesTest {
    public LocalVariablesTest() {
    }

    /**
     * double类型占用两个slot
     *
     */
    public String test01(String str1, String str2) {
        str1 = "null";
        str2 = "123";
        double weight = 130.5;
        char gender = '男';
        return str1 + str2;
    }

    /**
     * 槽重用
     */
    public void test02() {
        int a = 0;
        {
            int b = 0;
            b = a + 1;
        }
        int c = a + 1;
    }

}
