package com.shadow.garden.basic.geek.day01;

import java.util.Arrays;

/**
 * @Author lancer
 * @Date 2022/8/12 23:25
 * @Description 移动零：Leetcode283
 */
public class E03_MoveZeros {
    public static void main(String[] args) {
        int[] nums = new int[]{0, 1, 0, 3, 12};
        moveZeroes(nums);
        System.out.println(Arrays.toString(nums));
    }

    public static void moveZeroes(int[] nums) {
        int n = 0;
        for (int i = 0; i < nums.length; i++) {
            if (nums[i] != 0) {
                nums[n++] = nums[i];
            }
        }
        while (n < nums.length) {
            nums[n++] = 0;
        }
    }
}
