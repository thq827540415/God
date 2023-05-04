package com.ivi.basic.algorithm.demo01.class01;


/**
 * @Author lancer
 * @Date 2021/12/30 11:46 下午
 */

public class Code06_EvenTimesOddTimes {


    /**
     * arr中，只有一种数，出现奇数次
     *
     * @param arr
     */
    public static void printOddTimesNum1(int[] arr) {
        int eor = 0;
        for (int i = 0; i < arr.length; i++) {
            eor ^= arr[i];
        }
        System.out.println(eor);
    }


    /**
     * arr中，有两种数，出现奇数次
     *
     * @param arr
     */
    public static void printOddTimesNum2(int[] arr) {
        int eor = 0;
        // 两个奇数次的数异或的结果
        for (int i = 0; i < arr.length; i++) {
            eor ^= arr[i];
        }
        // eor = a ^ b
        // a != b => eor != 0
        // eor必然有一个位置上是1，不然，就相等了
        // 提取出最右的1
        int rightOne = eor & (~eor + 1);
        // eor'
        int onlyOne = 0;
        // 把最右边那一位为1的给异或起来，因为a和b会分开
        for (int i = 0; i < arr.length; i++) {
            if ((arr[i] & rightOne) != 0) {
                onlyOne ^= arr[i];
            }
        }
        System.out.println(onlyOne + " " + (eor ^ onlyOne));
    }


    /**
     * 计算出二进制中1的个数
     *
     * @param N
     * @return
     */

    public static int bit1counts(int N) {
        int count = 0;
        /*while (N != 0) {
            count++;
			int rightOne = N & (~N + 1);
			// 消除最后一个1
			N ^= rightOne;
        }*/

        while (N != 0) {
            count++;
            // N - 1将最后一位1转为0，且后面全为1
            // &表示去掉最后的1
            N &= N - 1;
        }
        return count;
    }

    public static void main(String[] args) {

        int[] arr1 = {3, 3, 2, 3, 1, 1, 1, 3, 1, 1, 1};
        printOddTimesNum1(arr1);

        int[] arr2 = {4, 3, 4, 2, 2, 2, 4, 1, 1, 1, 3, 3, 1, 1, 1, 4, 2, 2};
        printOddTimesNum2(arr2);

    }
}
