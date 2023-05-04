package com.ivi.algorithm.geek.day01;

// Leetcode 70
public class E03_ClimbingStairs {
    public static int climbStairs(int n) {
        /*int[] dp = new int[n + 1];
        dp[1] = 1;
        dp[2] = 2;
        for (int i = 3; i < dp.length; i++) {
            dp[i] = dp[i - 1] + dp[i - 2];
        }*/
        // return dp[n];


        int x = 1;
        int y = 2;
        int z = n == 1 ? x : y;
        for (int i = 3; i <= n; i++) {
            z = x + y;
            x = y;
            y = z;
        }
        return z;
    }

    public static void main(String[] args) {
        System.out.println(climbStairs(4));
    }
}
