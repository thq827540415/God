package com.shadow.garden.algorithm.sort;


import java.util.Arrays;

public class E01_ThreeSimpleSorts {

    /**
     * 每次选择一个最小/大数
     */
    private static void selectSort(int[] arr) {
        for (int i = 0; i < arr.length; i++) {
            int minIdx = i;
            for (int j = i; j < arr.length; j++) {
                minIdx = arr[minIdx] > arr[j] ? j : minIdx;
            }
            swap(arr, i, minIdx);
        }
    }

    /**
     * 后面e个是有序的
     */
    private static void bubbleSort(int[] arr) {
        // 如果没有发生交换，则说明数组有序，不需要继续比较了
        boolean flag = true;
        for (int e = arr.length - 1; e >= 0 && flag; e--) {
            flag = false;
            for (int i = 0; i < e; i++) {
                if (arr[i] > arr[i + 1]) {
                    swap(arr, i, i + 1);
                    flag = true;
                }
            }
        }
    }

    /**
     * 前面e个是有序的
     */
    private static void insertSort(int[] arr) {
        for (int e = 0; e < arr.length; e++) {
            for (int i = e; i > 0 && arr[i] < arr[i - 1]; i--) {
                swap(arr, i, i - 1);
            }
        }
    }

    public static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }


    public static void main(String[] args) {
        int[] arr1 = new int[]{2, 3, 1, 214, 5};
        int[] arr2 = new int[]{2, 3, 1, 214, 5};
        int[] arr3 = new int[]{2, 3, 1, 214, 5};
        selectSort(arr1);
        bubbleSort(arr2);
        insertSort(arr3);

        System.out.println(Arrays.toString(arr1));
        System.out.println(Arrays.toString(arr2));
        System.out.println(Arrays.toString(arr3));
    }
}
