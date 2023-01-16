package com.ava.basic.jvm.code.oom;

import java.util.ArrayList;
import java.util.List;

/**
 * 指定堆内存大小为20m，新生代大小为10m
 * -Xms20m -Xmx20m -Xmn10m -XX:+PrintGCDetails -XX:SurvivorRatio=8
 */
public class JavaHeap {

    private static class OOMObject {
    }

    public static void main(String[] args) {
        List<OOMObject> list = new ArrayList<>();
        while (true) {
            list.add(new OOMObject());
        }
    }
}
