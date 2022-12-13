package com.ava.basic.jvm.code.memoryleak;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author lancer
 * @Date 2022/4/6 21:12
 * @Description
 */
public class StaticCollectionDemo {

    static List<Object> list = new ArrayList<>();

    public void oomTests() {
        Object obj = new Object();
        list.add(obj);
    }


    public static void main(String[] args) {
       int a = -2;
        System.out.println(Integer.toBinaryString(a));
        System.out.println(Integer.toBinaryString(a >> 1));
    }
}
