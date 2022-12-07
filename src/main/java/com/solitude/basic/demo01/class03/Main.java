package com.solitude.basic.demo01.class03;

/**
 * @Author lancer
 * @Date 2022/1/3 12:34 上午
 * @Description
 */
public class Main {

    public static class Solution {
        int count = 0;
        public int InversePairs(int [] array) {
            if (array == null) {
                return count;
            }
            mergeSort(array, 0, array.length - 1);
            return count;
        }
        public void mergeSort(int[] array, int start, int end) {
            int mid = start + (end - start >> 1);
            if (start < end) {
                mergeSort(array, start, mid);
                mergeSort(array, mid + 1, end);
                merge(array, start, mid, end);
            }
        }

        public void merge(int[] array, int start, int mid, int end) {
            int[] arr = new int[end - start + 1];
            int s = start;
            int c = 0;
            int index = mid + 1;
            while (start <= mid && index <= end) {
                if (array[start] < array[index]) {
                    arr[c++] = array[start++];
                } else {
                    arr[c++] = array[index++];
                    // 只要后面比前面小，那么后面那个数会比[start, mid]的所有书小
                    count += mid - start + 1;
                    count = count % 1000000007;
                }
            }
            while (start <= mid) {
                arr[c++] = array[start++];
            }
            while (index <= end) {
                arr[c++] = array[index++];
            }
            for (int i : arr) {
                array[s++] = i;
            }
        }
    }
}
