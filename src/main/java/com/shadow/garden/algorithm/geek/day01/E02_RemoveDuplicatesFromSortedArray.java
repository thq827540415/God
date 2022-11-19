package com.shadow.garden.algorithm.geek.day01;

import java.util.Arrays;

/**
 * @Author lancer
 * @Date 2022/8/12 22:46
 * @Description 删除有序数组中的重复项：Leetcode26
 */
public class E02_RemoveDuplicatesFromSortedArray {
    public static void main(String[] args) {
        int[] nums = new int[]{0, 0, 1, 1, 1, 2, 2, 3, 3, 4};
        System.out.println(removeDuplicates(nums));
        System.out.println(Arrays.toString(nums));
    }

    public static int removeDuplicates(int[] nums) {
        int n = 0;
        for (int i = 0; i < nums.length; i++) {
            if (i == 0 || nums[i] != nums[i - 1]) {
                nums[n++] = nums[i];
            }
        }
        return n;
    }
}
