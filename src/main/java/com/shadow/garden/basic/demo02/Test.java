package com.shadow.garden.basic.demo02;

/**
 * @Author lancer
 * @Date 2022/4/11 21:41
 * @Description
 */
public class Test {
    public static void main(String[] args) {

        // System.out.println(floor(3, 3));
        // printQ();
        // System.out.println(maxSum());
        // oneZeroPackage();

        String str = "abcdefg";
        char[] chars = str.toCharArray();
        char tmp = chars[0];
        chars[0] = chars[1];
        chars[1] = tmp;
        str =new String(chars);
        System.out.println(str);
    }

    public static void oneZeroPackage() {
        int[] nums = new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4};
        int[] dp = new int[nums.length];
        int a = 100;
        int i1 = a % 100 == 0 ? a / 100 : a / 100 + 1;
        dp[0] = nums[0];

        for (int i = 1; i < nums.length; i++) {
            dp[i] = Math.max(nums[i], dp[i - 1] + nums[i]);
        }

        int max = Integer.MIN_VALUE;
        for (int j : dp) {
            System.out.print(j + " ");
            max = Math.max(max, j);
        }

        System.out.println("\n" + max);
    }

    public static int floor(int i, int j) {
        int[][] dp = new int[8][8];
        for (int m = 1; m <= i; m++) {
            for (int n = 1; n <= j; n++) {
                if (m == 1 && n == 1) {
                    dp[m][n] = 0;
                } else if (m == 1 || n == 1) {
                    dp[m][n] = 1;
                } else {
                    dp[m][n] = dp[m - 1][n] + dp[m][n - 1];
                }
            }
        }

        return dp[i][j];
    }

    static int maxSum() {
        int[][] nums = new int[][]{
                {0},
                {0, 7},
                {0, 3, 8},
                {0, 8, 1, 0},
                {0, 2, 7, 4, 4},
                {0, 4, 5, 2, 6, 5}
        };
        int[][] dp = new int[6][6];
        for (int i = 1; i <= 5; i++) {
            for (int j = 1; j <= i; j++) {
                dp[i][j] = Math.max(dp[i - 1][j - 1], dp[i - 1][j]) + nums[i][j];
            }
        }

        int max = -1;
        for (int j = 1; j < 6; j++) {
            max = Math.max(dp[5][j], max);
        }

        return max;
    }

    static int maxSum2() {
        int[][] nums = new int[][]{
                {0},
                {0, 7},
                {0, 3, 8},
                {0, 8, 1, 0},
                {0, 2, 7, 4, 4},
                {0, 4, 5, 2, 6, 5}
        };
        // 每一行的最大值
        int[] dp = new int[6];
        return -1;
    }


    public static void printQ() {
        for (int i = 1; i <= 7; i++) {
            for (int j = 1; j <= 7; j++) {
                System.out.print("* ");
            }
            System.out.println();
        }
    }
}
