package com.ivi.algorithm.sort;

import java.util.Arrays;

/**
 * @Author lancer
 * @Date 2021/12/31 12:08 上午
 */

// merge稳定
public class E03_MergeSort {
    public static void merge(int[] arr, int l, int mid, int h) {
        int[] help = new int[h - l + 1];
        int i = 0;

        int p1 = l;
        int p2 = mid + 1;
        while (p1 <= mid && p2 <= h) {
            help[i++] = arr[p1] <= arr[p2] ? arr[p1++] : arr[p2++];
        }
        // 左边元素放入help中
        while (p1 <= mid) {
            help[i++] = arr[p1++];
        }
        // 右边元素放入help中
        while (p2 <= h) {
            help[i++] = arr[p2++];
        }

        for (i = 0; i < help.length; i++) {
            arr[l + i] = help[i];
        }
    }

    /**
     * 递归方法实现
     */
    public static void mergeSort1(int[] arr) {
        process(arr, 0, arr.length - 1);
    }

    public static void process(int[] arr, int l, int h) {
        if (l >= h) {
            return;
        }
        int mid = l + ((h - l) >> 1);
        process(arr, l, mid);
        process(arr, mid + 1, h);
        merge(arr, l, mid, h);
    }

    /**
     * 非递归方法实现
     */
    public static void mergeSort2(int[] arr) {
        int mergeSize = 1;
        while (mergeSize < arr.length) {
            int l = 0;
            while (l < arr.length) {
                int mid = l + mergeSize - 1;
                if (mid >= arr.length) {
                    break;
                }
                int h = Math.min(mid + mergeSize, arr.length - 1);
                merge(arr, l, mid, h);
                l = h + 1;
            }
            mergeSize <<= 1;
        }
    }

    public static void main(String[] args) {
        int[] ints = {4, 1, 2, 5, 6, 3};
        mergeSort2(ints);
        System.out.println(Arrays.toString(ints));
    }
}
