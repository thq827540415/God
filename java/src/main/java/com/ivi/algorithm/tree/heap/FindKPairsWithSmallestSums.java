package com.ivi.algorithm.tree.heap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;

// leet code 373 查找和最小的K对数字
public class FindKPairsWithSmallestSums {
    public List<List<Integer>> kSmallestPairs(int[] nums1, int[] nums2, int k) {
        List<List<Integer>> res = new ArrayList<>();
        // 保存索引，对索引对应的值的和比较大小
        PriorityQueue<int[]> queue = new PriorityQueue<>(
                k,
                (arr1, arr2) ->
                        Integer.compare(nums1[arr1[0]] + nums2[arr1[1]], nums1[arr2[0]] + nums2[arr2[1]]));

        for (int i = 0; i < Math.min(nums1.length, k); i++) {
            queue.offer(new int[]{i, 0});
        }

        while (k-- > 0 && !queue.isEmpty()) {
            int[] indexes = queue.poll();
            res.add(Arrays.asList(nums1[indexes[0]], nums2[indexes[1]]));
            if (indexes[1] + 1 < nums2.length) {
                queue.offer(new int[] {indexes[0], indexes[1] + 1});
            }
        }

        return res;
    }

    public static void main(String[] args) {
    }
}
