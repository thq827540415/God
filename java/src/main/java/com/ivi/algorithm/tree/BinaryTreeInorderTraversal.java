package com.ivi.algorithm.tree;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

// leet code 94 二叉树的中序遍历
public class BinaryTreeInorderTraversal {
    public static void main(String[] args) {
    }

    // 递归写法
    private List<Integer> inorder1(TreeNode root) {
        List<Integer> ans = new ArrayList<>();
        demo(root, ans);
        return ans;
    }

    private void demo(TreeNode root, List<Integer> lst) {
        if (root == null) {
            return;
        }
        demo(root.left, lst);
        lst.add(root.val);
        demo(root.right, lst);
    }


    // 递推写法，使用栈
    private List<Integer> inorder2(TreeNode root) {
        List<Integer> ans = new ArrayList<>();
        if (root == null) {
            return ans;
        }
        Deque<TreeNode> stack = new LinkedList<>();

        while (root != null || !stack.isEmpty()) {
            while (root != null) {
                stack.push(root);
                root = root.left;
            }

            root = stack.pop();
            ans.add(root.val);
            root = root.right;
        }

        return ans;
    }

    // Morris算法，减少空间复杂度
    private List<Integer> inorder3(TreeNode root) {
        List<Integer> ans = new ArrayList<>();
        if (root == null) {
            return ans;
        }
        // 利用pre指针进行回溯
        TreeNode pre;

        while (root != null) {
            if (root.left == null) {
                // 没有左孩子，直接访问右孩子
                ans.add(root.val);
                root = root.right;
            } else {
                pre = root.left;
                while (pre.right != null && pre.right != root) {
                    pre = pre.right;
                }

                if (pre.right == null) {
                    pre.right = root;
                    root = root.left;
                } else {
                    // 此时说明绕回来了，则断掉链接，阻止重复处理
                    pre.right = null;
                    ans.add(root.val);
                    root = root.right;
                }
            }
        }

        return ans;
    }

    // 后序遍历
    private void postorderTraversal(TreeNode root) {
        if (root == null) {
            return;
        }
        Deque<TreeNode> stack = new LinkedList<>();
        TreeNode pre = null;
        do {
            while (root != null) {
                stack.push(root);
                root = root.left;
            }

            root = stack.pop();
            if (root.right != null && root.right != pre) {
                stack.push(root);
                root = root.right;
            } else {
                pre = root;
                // 防止重复压栈
                root = null;
                // 处理逻辑
                // ...

            }
        } while(!stack.isEmpty());
    }
}
