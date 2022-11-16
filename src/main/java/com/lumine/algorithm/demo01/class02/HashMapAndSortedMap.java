package com.lumine.algorithm.demo01.class02;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

// 底层组织
public class HashMapAndSortedMap {

    public static class Node {
        public int value;

        public Node(int v) {
            value = v;
        }
    }


    public static void main(String[] args) {
        // key - value
        // 值传递
        HashMap<Integer, String> map = new HashMap<>();
        map.put(1, "我是1");
        map.put(2, "我是2");
        map.put(3, "我是3");
        map.put(4, "我是4");
        map.put(5, "我是5");
        map.put(6, "我是6");

        // key
        HashSet<String> set = new HashSet<>();
        set.add("abc");
        System.out.println(set.contains("abc"));
        set.remove("abc");

        // 哈希表，增删改查，在使用时，时间复杂度O(1)
        int a = 100000;
        int b = 100000;
        System.out.println(a == b);// true

        Integer c = 100000;
        Integer d = 100000;
        System.out.println(c.equals(d));// true

        Integer e = 127;// -128 ~ 127使用值传递，超出使用引用传递
        Integer f = 127;
        System.out.println(e == f);// true


        // 非基础类型的key引用传递
        HashMap<Node, String> map2 = new HashMap<>();
        Node node1 = new Node(1);
        Node node2 = new Node(2);
        map2.put(node1, "我是node1");
        map2.put(node2, "我是node2");

        System.out.println("=============================");

        // 有序表 时间复杂度O(logN)
        TreeMap<Integer, String> treeMap = new TreeMap<>();
        treeMap.put(3, "我是3");
        treeMap.put(8, "我是8");
        treeMap.put(4, "我是4");
        treeMap.put(5, "我是5");
        treeMap.put(7, "我是7");

        System.out.println(treeMap.firstKey());
        System.out.println(treeMap.lastKey());

        // key <= 5 离5最近的
        System.out.println(treeMap.floorKey(5));
        // key >= 5
        System.out.println(treeMap.ceilingKey(5));
    }
}
