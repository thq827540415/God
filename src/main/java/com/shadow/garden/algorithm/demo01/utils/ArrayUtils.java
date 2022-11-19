package com.shadow.garden.algorithm.demo01.utils;

/**
 * @Author lancer
 * @Date 2021/12/31 12:09 上午
 */

public class ArrayUtils {
    public static boolean isArrayNotIllegal(int[] arr) {
        return arr == null || arr.length < 2;
    }
}
