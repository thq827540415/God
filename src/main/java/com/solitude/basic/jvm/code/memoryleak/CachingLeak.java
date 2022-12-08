package com.solitude.basic.jvm.code.memoryleak;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author lancer
 * @Date 2022/4/6 21:56
 * @Description 内存泄露
 */
public class CachingLeak {
    static Map<String, String> wMap = new WeakHashMap<String, String>();
    static Map<String, String> map = new HashMap<>();

    public static void main(String[] args) {
        init();
        testWeakHashMap();
        testHashMap();
    }

    public static void init() {
        String ref1 = new String("weakMap1");
        String ref2 = new String("weakMap2");
        String ref3 = new String("map1");
        String ref4 = new String("map2");

        wMap.put(ref1, "cacheObject1");
        wMap.put(ref2, "cacheObject2");
        map.put(ref3, "cacheObject3");
        map.put(ref4, "cacheObject4");
        System.out.println("String引用ref1，ref2，ref3，ref4消失");
    }

    public static void testWeakHashMap() {
        System.out.println("WeakHashMap GC之前");
        for (Map.Entry<String, String> o : wMap.entrySet()) {
            System.out.println(o);
        }
        System.gc();
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("WeakHashMap GC之后");
        for (Map.Entry<String, String> o : wMap.entrySet()) {
            System.out.println(o);
        }
    }

    public static void testHashMap() {
        System.out.println("HashMap GC之前");
        for (Map.Entry<String, String> o : map.entrySet()) {
            System.out.println(o);
        }
        System.gc();
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("HashMap GC之后");
        for (Map.Entry<String, String> o : map.entrySet()) {
            System.out.println(o);
        }
    }
}
