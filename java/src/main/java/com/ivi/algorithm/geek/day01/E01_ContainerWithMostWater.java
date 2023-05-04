package com.ivi.algorithm.geek.day01;

// Leetcode 11
public class E01_ContainerWithMostWater {
    public static int maxArea(int[] height) {
        int l = 0;
        int h = height.length - 1;
        int res = 0;
        while (l < h) {
            int min = Math.min(height[l], height[h]);
            res = Math.max(res, (h - l) * min);
            while (l < h && height[l] <= min) {
                l++;
            }
            while (l < h && height[h] <= min) {
                h--;
            }
        }
        return res;
    }

    public static void main(String[] args) {
        System.out.println(maxArea(new int[]{1, 8, 6, 2, 5, 4, 8, 3, 7}));
    }
}
