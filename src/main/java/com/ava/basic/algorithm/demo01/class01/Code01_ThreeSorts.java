package com.ava.basic.algorithm.demo01.class01;

import java.util.Arrays;

/**
 * @Author lancer
 * @Date 2021/12/30 5:38 下午
 */
public class Code01_ThreeSorts {
    public static void main(String[] args) {

        int[] arr1 = new int[]{2, 3, 1, 214, 5};
        int[] arr2 = new int[]{2, 3, 1, 214, 5};
        int[] arr3 = new int[]{2, 3, 1, 214, 5};
        selectionSort(arr1);
        bubbleSort(arr2);
        insertionSort(arr3);

        Arrays.stream(arr1).forEach(num -> System.out.print(num + " "));
        System.out.println();
        Arrays.stream(arr2).forEach(num -> System.out.print(num + " "));
        System.out.println();
        Arrays.stream(arr3).forEach(num -> System.out.print(num + " "));
    }

    /**
     * 选择排序
     *
     * @param arr
     */
    public static void selectionSort(int[] arr) {


        for (int i = 0; i < arr.length; i++) {
            int minIndex = i;
            for (int j = i + 1; j < arr.length; j++) {
                minIndex = arr[j] < arr[minIndex] ? j : minIndex;
            }
            swap(arr, i, minIndex);
        }
    }

    /**
     * 冒泡排序
     *
     * @param arr
     */
    public static void bubbleSort(int[] arr) {


        // 如果没有发生交换，则说明数组有序，不需要继续比较了
        boolean hasChange = true;
        for (int i = arr.length - 1; i >= 0 && hasChange; i--) {
            hasChange = false;
            for (int j = 0; j < i; j++) {
                if (arr[j] > arr[j + 1]) {
                    swap(arr, j, j + 1);
                    hasChange = true;
                }
            }
        }
    }

    /**
     * 插入排序
     *
     * @param arr
     */
    public static void insertionSort(int[] arr) {


        for (int i = 1; i < arr.length; i++) {
            for (int j = i - 1; j >= 0 && arr[j] > arr[j + 1]; j--) {
                swap(arr, j, j + 1);
            }
        }
    }


    public static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }
}
