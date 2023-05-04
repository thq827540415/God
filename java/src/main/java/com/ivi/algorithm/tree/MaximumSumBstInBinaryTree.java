package com.ivi.algorithm.tree;

// leet code 1373
public class MaximumSumBstInBinaryTree {
    // 树型dp? + DFS
    private int ans = 0;

    private static class State {
        boolean isBST;
        // 当前节点所在子搜索树的最大值，最小值，节点和
        int max, min, sum;

        public State(boolean isBST, int max, int min, int sum) {
            this.isBST = isBST;
            this.max = max;
            this.min = min;
            this.sum = sum;
        }
    }

    public int maxSumBST1(TreeNode root) {
        logic(root);
        return ans;
    }

    private State logic(TreeNode root) {
        if (root == null) {
            return new State(true, Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
        }

        State stateL = logic(root.left);
        State stateR = logic(root.right);

        boolean isBST =
                stateL.isBST
                        && stateR.isBST
                        && root.val > stateL.max
                        && root.val < stateR.min;

        int sum = 0;
        if (isBST) {
            sum = stateL.sum + root.val + stateR.sum;
            ans = Math.max(ans, sum);
        }

        return new State(
                isBST,
                Math.max(root.val, Math.max(stateL.max, stateR.max)),
                Math.min(root.val, Math.min(stateL.min, stateR.min)),
                sum);
    }

    // 常规递归
    private int max = 0;

    public int maxSumBST(TreeNode root) {
        maxSubBST(root);
        return max;
    }

    private void maxSubBST(TreeNode root) {
        // 只要是二叉搜索树，则左右子树必定也是二叉树
        if (isBST(root, Integer.MIN_VALUE, Integer.MAX_VALUE)) {
            sum(root);
            return;
        }

        maxSumBST(root.left);
        maxSumBST(root.right);
    }

    // dfs判断是否是BST，此处Integer.MIN_VALUE < node.val < Integer.MAX_VALUE，
    private boolean isBST(TreeNode root, int lower, int upper) {
        if (root == null) {
            return true;
        }

        return root.val > lower
                && root.val < upper
                && isBST(root.left, lower, root.val)
                && isBST(root.right, root.val, upper);
    }

    private int sum(TreeNode node) {
        if (node == null) {
            return 0;
        }
        // 每个子二叉搜索树的和
        int val = node.val + sum(node.left) + sum(node.right);
        // sum过程中会出现最大值
        max = Math.max(max, val);
        return val;
    }

    public static void main(String[] args) {
        TreeNode root = new TreeNode(-4);
        root.left = new TreeNode(-10);
        root.right = new TreeNode(1);

        System.out.println(new MaximumSumBstInBinaryTree().maxSumBST1(root));
    }
}
