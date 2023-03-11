package com.ava.basic.grammar;

import java.util.Arrays;

/**
 * @Author lancer
 * @Date 2023/3/7 20:35
 * @Description
 */
public class EnumLearn {
    enum Size {
        // 枚举常量需要定义在最前面，同时根据位置还有个从0开始的顺序标号ordinal
        SMALL("S"), MEDIUM("M"), LARGE("L"), EXTRA_LARGE("XL");

        private final String shortName;

        public String getShortName() {
            return this.shortName;
        }

        // 构造器只能私有
        private Size(String shortName) {
            this.shortName = shortName;
        }
    }

    public static void main(String[] args) {
        // 输出的不是S而是SMALL
        System.out.println(Size.SMALL);

        // 输出XL
        System.out.println(Size.EXTRA_LARGE.getShortName());

        // 将字符串MEDIUM转化成Size.MEDIUM，以下两者等价
        System.out.println(Size.valueOf("MEDIUM"));
        System.out.println(Enum.valueOf(Size.class, "MEDIUM"));

        // 除了Enum，每个枚举类型都有一个静态values方法
        System.out.println(Arrays.toString(Size.values()));

        // 该常量的位置
        System.out.println(Size.LARGE.ordinal());

        // 比较常量所在的位置，以下两者等价
        System.out.println(Size.SMALL.compareTo(Size.LARGE));
        System.out.println(Size.SMALL.ordinal() - Size.LARGE.ordinal());
    }
}
