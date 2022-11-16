package com.lumine.algorithm.demo01.class01;

/**
 * @Author lancer
 * @Date 2021/12/30 10:43 下午
 * 二分查找，判断是否存在
 */

public class Code02_BSExist {

    public static boolean exist(int[] sortedArr, int num) {
        if (sortedArr == null || sortedArr.length == 0) {
            return false;
        }
        int l = 0;
        int r = sortedArr.length - 1;
        int mid;
        // l..r
        while (l <= r) {
            // 四则运算符优先级高于位运算符
            mid = l + (r - l >> 1);
            if (sortedArr[mid] == num) {
                return true;
            } else if (sortedArr[mid] > num) {
                r = mid - 1;
            } else {
                l = mid + 1;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        int[] arr = new int[]{1, 2, 3, 4, 5};
        System.out.println(exist(arr, 7));
    }

}
