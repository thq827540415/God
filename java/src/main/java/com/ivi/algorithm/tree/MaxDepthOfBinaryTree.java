package com.ivi.algorithm.tree;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MaxDepthOfBinaryTree {
    public static void main(String[] args) {
    }

    // BFS，队列实现
    private int maxDepthBFS(TreeNode root) {
        int ans = 0;
        if (root == null) {
            return ans;
        }

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            int size = queue.size();
            while (size-- > 0 && !queue.isEmpty()) {
                root = queue.poll();
                if (root.left != null) {
                    queue.offer(root.left);
                }
                if (root.right != null) {
                    queue.offer(root.right);
                }
            }
            ans++;
        }

        return ans;
    }

    // DFS
    private int maxDepthDFS(TreeNode root) {
        return root == null
                ? 0
                : 1 + Math.max(maxDepthDFS(root.left), maxDepthDFS(root.right));
    }


    private static class NTreeNode {
        int value;
        List<NTreeNode> children;
    }

    // 拓展到n叉树，递归实现
    private int maxDepthOfN(NTreeNode root) {
        if (root == null) {
            return 0;
        }
        int max = 0;
        for (NTreeNode child : root.children) {
            max = Math.max(maxDepthOfN(child), max);
        }

        return 1 + max;
    }
}
