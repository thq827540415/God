package com.shadow.garden.algorithm.geek.day01;

import java.util.Arrays;

/**
 * @Author lancer
 * @Date 2022/8/13 09:17
 * @Description 移除元素Leetcode27
 */
public class E04_RemoveElements {
    public static void main(String[] args) {
        int[] nums = new int[]{0, 1, 2, 2, 3, 0, 4, 2};
        System.out.println(removeElement(nums, 2));
        // 前removeElement位
        System.out.println(Arrays.toString(nums));
    }

    public static int removeElement(int[] nums, int val) {
        int n = 0;
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] != val) {
                nums[n++] = nums[i];
            }
        }
        return n;
    }
}
