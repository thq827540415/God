package com.solitude.basic.sort;

import com.solitude.basic.datastruct.LinkedNode;
import java.util.Arrays;

public class E02_QuickSort {
    private static void process(int[] arr, int L, int R) {
        if (L >= R) {
            return;
        }
        // 基准左边 <= pivot <= 基准右边
        int pivot = doPartition(arr, L, R);
        // int pivot = partition(arr, L, R);
        process(arr, L, pivot - 1);
        process(arr, pivot + 1, R);
    }

    /**
     * 取L为初始基准
     */
    private static int doPartition(int[] arr, int L, int R) {
        int k = L;
        while (L < R) {
            // 从右边找到比arr[k]小的数
            while (L < R && arr[k] < arr[R]) {
                R--;
            }
            // 从左边找到比arr[k]大的数
            while (L < R && arr[k] > arr[L]) {
                L++;
            }
            swap(arr, L, R);
        }
        swap(arr, k, L);
        return L;
    }


    private static int partition(int[] arr, int L, int R) {
        if (L == R) {
            return L;
        }
        int lessEqual = L - 1;
        while (L < R) {
            // 把arr[R]当作基准的值
            if (arr[L] <= arr[R]) {
                swap(arr, L, ++lessEqual);
            }
            L++;
        }
        swap(arr, ++lessEqual, R);
        return lessEqual;
    }

    private static void swap(int[] arr, int i, int j) {
        int tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }


    public static void main(String[] args) {
        int[] arr = new int[]{21, 3, 8, 12, 1};
        process(arr, 0, arr.length - 1);
        System.out.println(Arrays.toString(arr));
    }

    // 单链表的快速排序
    private static class LinkedQuickSort {

        private static <T extends Comparable<T>> void process(LinkedNode<T> start, LinkedNode<T> end) {
            if (start == null || start.next == end ||start == end) return;
            LinkedNode<T> provit = partition(start, end);
            process(start, provit);
            process(provit.next, end);
        }

        private static <T extends Comparable<T>> LinkedNode<T> partition(LinkedNode<T> start, LinkedNode<T> end) {
            // 一个用于定位的指针
            LinkedNode<T> base = start;
            // 一个用于跑动的指针
            LinkedNode<T> cur = start.next;
            while (cur != end) {
                if (start.val.compareTo(cur.val) > 0) {
                    base = base.next;
                    LinkedNode.swap(base, cur);
                }
                cur = cur.next;
            }
            LinkedNode.swap(start, base);
            return base;
        }

        public static void main(String[] args) {
            Integer[] ints = {4, 1, 3, 12, 2};
            LinkedNode<Integer> head = LinkedNode.generate(ints);
            LinkedNode.print(head);
            process(head, null);
            LinkedNode.print(head);
        }
    }
}
