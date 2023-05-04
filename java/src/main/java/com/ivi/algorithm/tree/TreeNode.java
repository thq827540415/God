package com.ivi.algorithm.tree;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TreeNode {
    int val;
    TreeNode left, right;

    public TreeNode(int val) {
        this.val = val;
    }

    public TreeNode(int val, TreeNode left, TreeNode right) {
        this(val);
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return String.format("val: %s", val);
    }
}
