package com.ava.basic.jvm.code.oom;

import java.util.HashSet;
import java.util.Set;

/**
 * JDK7或更高版本运行该代码都不会得到PermGen space的OOM异常，验证了JDK7开始就把常量池移出了方法区
 * -Xms6m -Xmx6m会得到Java heap space的OOM，表示从JDK7开始运行时常量池就移动到了Java堆中
 */
public class RuntimeConstantPool {
    /**
     * -XX:PermSize=6m -XX:MaxPermSize=6m
     */
    public static void main(String[] args) {
        // 使用Set保持着常量池的引用，避免Full GC回收常量池的行为
        Set<String> set = new HashSet<>();
        short i = 0;
        while (true) {
            set.add(String.valueOf(i++).intern());
        }
    }
}
