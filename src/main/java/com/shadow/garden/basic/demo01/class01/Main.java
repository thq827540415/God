package com.shadow.garden.basic.demo01.class01;

import java.util.Arrays;

/**
 * @Author lancer
 * @Date 2022/1/1 5:51 下午
 */
public class Main {
    public static int[] countBits(int n) {
        int[] res = new int[n + 1];
        for (int i = 0; i <= n; i++) {
            int count = 0;
            int num = i;
            while (num != 0) {
                count++;
                num &= (num - 1);
            }
            res[i] = count;
        }
        return res;
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(countBits(2)));
        System.out.println();
        test();
    }

    public static void test() {
        int[] arr = new int[] {2, 3, 14, 1};
        for (int i = 0; i < arr.length; i++) {
            for (int j = i - 1; j >= 0 && arr[j] > arr[j + 1]; j--) {
                int tmp = arr[j];
                arr[j] = arr[j + 1];
                arr[j + 1] = tmp;
            }
        }
        Arrays.stream(arr).forEach(System.out::println);
    }
}
