package com.ivi.algorithm.geek.day01;

import java.util.*;

// Leetcode 15
// 固定一个数，跑两个指针，也可以等价两数之和 -> a + b = -c
// 同理4数之和，固定两个数，跑两个指针
public class E04_3Sum {
    public static List<List<Integer>> threeSum(int[] nums) {
        List<List<Integer>> res = new ArrayList<>();
        Arrays.sort(nums);
        if (nums[0] > 0 || nums[nums.length - 1] < 0) {
            return res;
        }
        for (int i = 0; i < nums.length - 2; i++) {
            if (nums[i] > 0) {
                return res;
            }
            if (i == 0 || nums[i] != nums[i - 1]) {
                twoSum(res, nums, i + 1, -nums[i]);
            }
        }
        return res;
    }

    /**
     * (nums is sorted && start > 0) == true
     */
    static void twoSum(List<List<Integer>> res, int[] nums, int start, int target) {
        for (int l = start, h = nums.length - 1; l < h; ) {
            int sum = nums[l] + nums[h];
            if (sum > target) {
                h--;
            } else if (sum < target) {
                l++;
            } else {
                if (start == 0) {
                    // 用于测试单个twoSum方法
                    res.add(Arrays.asList(nums[l], nums[h]));
                } else {
                    res.add(Arrays.asList(nums[start - 1], nums[l], nums[h]));
                }
                while (l < h && nums[h] == nums[h - 1]) h--;
                while (l < h && nums[l] == nums[l + 1]) l++;
                h--;
                l++;
            }
        }
    }

    public static void main(String[] args) {
        System.out.println(threeSum(new int[]{-4, -2, -2, -2, 0, 1, 2, 2, 2, 3, 3, 4, 4, 6, 6}));
        System.out.println(threeSum(new int[]{-4, -1, -1, 0, 1, 2}));
        System.out.println(threeSum(new int[]{-2, -1, -1, 0, 1, 1, 2}));
        System.out.println(threeSum(new int[]{0, 1, 1}));
        System.out.println(threeSum(new int[]{0, 0, 0}));
    }
}
