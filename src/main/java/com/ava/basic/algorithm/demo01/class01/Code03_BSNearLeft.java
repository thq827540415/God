package com.ava.basic.algorithm.demo01.class01;

/**
 * @Author lancer
 * @Date 2021/12/30 11:38 下午
 */

public class Code03_BSNearLeft {

    /**
     * 在arr上，找满足>=value的最左位置
     *
     * @param arr
     * @param value
     * @return
     */
    public static int nearestIndex(int[] arr, int value) {
        int L = 0;
        int R = arr.length - 1;
        // 记录最左的对号
        int index = -1;
        while (L <= R) {
            int mid = L + ((R - L) >> 1);
            if (arr[mid] >= value) {
                index = mid;
                R = mid - 1;
            } else {
                L = mid + 1;
            }
        }
        return index;
    }
}
