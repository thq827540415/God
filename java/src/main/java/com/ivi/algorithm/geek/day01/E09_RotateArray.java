package com.ivi.algorithm.geek.day01;

import java.util.Arrays;

// Leetcode 189
public class E09_RotateArray {

    // 空间复杂度为O(1)解决
    public static void rotate(int[] nums, int k) {
        k = k % nums.length;
        // 1. 反转所有的元素 4 3 2 1
        reverse(nums, 0, nums.length - 1);
        // 2. 反转前面一部分 3 4  2 1
        reverse(nums, 0, k - 1);
        // 3. 反转后面一部分 3 4  1 2
        reverse(nums, k, nums.length - 1);
    }

    // 双指针
    static void reverse(int[] nums, int l, int h) {
        while (l < h) {
            int tmp = nums[l];
            nums[l] = nums[h];
            nums[h] = tmp;
            l++;
            h--;
        }
    }

    public static void main(String[] args) {
        int[] a = {1, 2, 3, 4, 5, 6, 7};
        int[] b = {-1, -100, 3, 99};
        rotate(a, 3);
        rotate(b, 2);
        // 5, 6, 7, 1, 2, 3, 4
        System.out.println(Arrays.toString(a));
        // 3, 99, -1, -100
        System.out.println(Arrays.toString(b));
    }
}
