package com.ivi.algorithm.tree;

import java.util.Deque;
import java.util.LinkedList;

// leet code 98 验证二叉搜索树
// Integer.MIN_VALUE <= node.val <= Integer.MAX_VALUE，所以要使用Long
public class ValidateBinarySearchTree {
    // 使用递归
    public boolean isValidateBST(TreeNode root) {
        return isValidateBST(root, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private boolean isValidateBST(TreeNode root, long lower, long upper) {
        if (root == null) {
            return true;
        }

        return root.val > lower
                && root.val < upper
                && isValidateBST(root.left, lower, root.val)
                && isValidateBST(root.right, root.val, upper);
    }

    // 使用中序遍历判断二叉搜索树
    private boolean isBST(TreeNode root) {
        if (root == null) {
            return true;
        }

        Deque<TreeNode> stack = new LinkedList<>();
        // 核心：上一个中序值
        int inorder = Integer.MIN_VALUE;

        while (!stack.isEmpty() || root != null) {
            while (root != null) {
                stack.push(root);
                root = root.left;
            }

            root = stack.pop();
            if (root.val <= inorder) {
                return false;
            }
            inorder = root.val;
            root = root.right;
        }
        return true;
    }

    // 记录每个node的状态
    private static class Tuple3 {
        boolean f0;
        long f1, f2;

        public Tuple3(boolean f0, long f1, long f2) {
            this.f0 = f0;
            this.f1 = f1;
            this.f2 = f2;
        }
    }

    public boolean isValidBST(TreeNode root) {
        return recur(root).f0;
    }

    // flag, leftMax, rightMin
    private Tuple3 recur(TreeNode root) {
        if (root == null) {
            return new Tuple3(true, Long.MIN_VALUE, Long.MAX_VALUE);
        }
        // 左边
        Tuple3 left = recur(root.left);

        // 右边
        Tuple3 right = recur(root.right);

        return new Tuple3(
                left.f0
                        && right.f0
                        && root.val > left.f1
                        && root.val < right.f2,
                compare(root.val, left.f1, right.f1, true),
                compare(root.val, left.f2, right.f2, false));
    }

    private long compare(long a, long b, long c, boolean isMax) {
        return isMax
                ? Math.max(a, Math.max(b, c))
                : Math.min(a, Math.min(b, c));
    }
}
