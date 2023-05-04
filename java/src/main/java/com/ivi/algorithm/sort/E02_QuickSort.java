package com.ivi.algorithm.sort;

import com.ivi.datastruct.ListNode;

import java.util.Arrays;
import java.util.Random;

// quick不稳定
// 最好时间复杂度（选择的基准刚好为中位数）等同于归并排序 N * logN, 最坏（基准为最大值或最小值）等同于选择排序 N * N
public class E02_QuickSort {
    private static void process(int[] arr, int l, int h) {
        if (l >= h) {
            return;
        }
        // 需要满足基准左边 <= pivot <= 基准右边
        int pivot = doPartition(arr, l, h);
        // int pivot = partition(arr, l, h);
        process(arr, l, pivot - 1);
        process(arr, pivot + 1, h);
    }

    // 取L为初始基准
    private static int doPartition(int[] arr, int l, int h) {
//        int k = l++;
       int k = new Random().nextInt(h - l + 1) + l;
        while (l < h) {
            // 从右边找到比arr[k]小的数
            while (l < h && arr[k] <= arr[h]) {
                h--;
            }
            // 从左边找到比arr[k]大的数
            while (l < h && arr[k] >= arr[l]) {
                l++;
            }
            swap(arr, l, h);
        }
        swap(arr, k, l);
        return l;
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
        int[] arr = new int[]{4, 1, 2, 5, 6, 3};
        process(arr, 0, arr.length - 1);
        System.out.println(Arrays.toString(arr));
    }

    // 单链表的快速排序，只交换值，不交换节点
    private static class LinkedQuickSort {
        private static <T extends Comparable<T>> void process(ListNode<T> start, ListNode<T> end) {
            if (start == null || start.next == end || start == end) return;
            ListNode<T> provit = partition(start, end);
            process(start, provit);
            process(provit.next, end);
        }

        private static <T extends Comparable<T>> ListNode<T> partition(ListNode<T> start, ListNode<T> end) {
            // 一个用于定位的指针
            ListNode<T> base = start;
            // 一个用于跑动的指针
            ListNode<T> cur = start.next;
            while (cur != end) {
                if (start.val.compareTo(cur.val) > 0) {
                    base = base.next;
                    ListNode.swap(base, cur);
                }
                cur = cur.next;
            }
            ListNode.swap(start, base);
            return base;
        }

        public static void main(String[] args) {
            Integer[] ints = {4, 1, 3, 12, 2};
            ListNode<Integer> head = ListNode.generate(ints);
            ListNode.print(head);
            process(head, null);
            ListNode.print(head);
        }
    }
}
