package com.shadow.garden.algorithm.demo01.class02;

/**
 * @Author lancer
 * @Date 2022/1/1 11:06 下午
 * @Description 用递归求arr中最大值
 */
public class Code08_GetMax {

    public static int getMax(int[] arr) {
        return process(arr, 0, arr.length - 1);
    }

    /**
     * arr[L..R]范围上求最大值
     *
     * @param arr
     * @param L
     * @param R
     * @return
     */
    public static int process(int[] arr, int L, int R) {
        // arr[L..R]范围上只有一个数，直接返回，base case
        if (L == R) {
            return arr[L];
        }
        //  L..mid  mid+1...R
        // int mid = (L+R)/2
        int mid = L + ((R - L) >> 1);
        int leftMax = process(arr, L, mid);
        int rightMax = process(arr, mid + 1, R);
        return Math.max(leftMax, rightMax);
    }

    public static void main(String[] args) {
        int[] arr = {3, 1, 23, 45, 2};
        System.out.println(getMax(arr));
    }
}
