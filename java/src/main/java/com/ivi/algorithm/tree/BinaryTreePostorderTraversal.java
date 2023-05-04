package com.ivi.algorithm.tree;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class BinaryTreePostorderTraversal {

    // 递归写法
    public List<Integer> postorderTraversal1(TreeNode root) {
        List<Integer> ans = new ArrayList<>();
        demo(root, ans);
        return ans;

    }

    private void demo(TreeNode root, List<Integer> ans) {
        if (root == null) {
            return;
        }

        demo(root.left, ans);
        demo(root.right, ans);
        ans.add(root.val);
    }

    // 递推写法，使用栈
    public List<Integer> postorderTraversal2(TreeNode root) {
        List<Integer> ans = new ArrayList<>();
        if (root == null) {
            return ans;
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
                // 处理逻辑
                ans.add(root.val);
                // 防止重复压栈
                root = null;
            }
        } while (!stack.isEmpty());

        return ans;
    }
}
