package com.lumine.algorithm.demo01.class01;

/**
 * @Author lancer
 * @Date 2021/12/30 11:42 下午
 */

public class Code04_BSNearRight {

    /**
     * 在arr上，找满足<=value的最右位置
     *
     * @param arr
     * @param value
     * @return
     */
    public static int nearestIndex(int[] arr, int value) {
        int L = 0;
        int R = arr.length - 1;
        // 记录最右的对号
        int index = -1;
        while (L <= R) {
            int mid = L + ((R - L) >> 1);
            if (arr[mid] <= value) {
                index = mid;
                L = mid + 1;
            } else {
                R = mid - 1;
            }
        }
        return index;
    }
}
