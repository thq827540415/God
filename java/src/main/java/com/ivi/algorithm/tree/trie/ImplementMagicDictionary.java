package com.ivi.algorithm.tree.trie;


import java.util.*;
import java.util.stream.Collectors;

// leet code 676 实现一个魔法字典
public class ImplementMagicDictionary {
    private static class TrieTreeNode {
        boolean isFinished;
        TrieTreeNode[] child;

        public TrieTreeNode() {
            this.isFinished = false;
            this.child = new TrieTreeNode[26];
        }
    }

    TrieTreeNode root;

    public ImplementMagicDictionary() {
        root = new TrieTreeNode();
    }

    /**
     * 构建一颗Trie树
     */
    public void buildDict(String[] dictionary) {
        for (String word : dictionary) {
            // 每个单词，每次都从根结点开始
            TrieTreeNode curr = root;
            for (int i = 0; i < word.length(); i++) {
                // 获取单词中第i个字母的下标
                int idx = word.charAt(i) - 'a';
                if (curr.child[idx] == null) {
                    curr.child[idx] = new TrieTreeNode();
                }
                curr = curr.child[idx];
            }

            // 该单词构建完成
            curr.isFinished = true;
        }
    }

    public boolean search(String searchWord) {
        // 单词，从那个节点开始找，从单词第几个字母位置开始找，是否已经替换过一次字符
        return dfs(searchWord, root, 0, false);
    }

    private boolean dfs(String word, TrieTreeNode node, int pos, boolean modified) {
        if (pos == word.length()) {
            // 最后一个字母应该是finished 和 该单词modified
            return modified && node.isFinished;
        }

        // -------------------------------- 第一个递归找路径
        // 第pos个字母，从0开始的下标
        int idx = word.charAt(pos) - 'a';
        if (node.child[idx] != null) {
            // 查找下一个字母，直到最后一个字母
            if (dfs(word, node.child[idx], pos + 1, modified)) {
                // 一步为true，步步为true
                return true;
            }
        }

        // -------------------------------- 第二个递归找
        // 如果word现在pos + 1的字母还没修改过时
        // 从最后一个字母开始修改
        if (!modified) {
            for (int i = 0; i < 26; i++) {
                // 随意更换25个字母中的一个，并且该字母存在
                if (i != idx && node.child[i] != null) {
                    if (dfs(word, node.child[i], pos + 1, true)) {
                        // 存在修改一个字母的且还在字典中的，返回给第一个递归
                        return true;
                    }
                }
            }
        }
        // 说明此路不通，回到上一个字母
        return false;
    }

    public static void main(String[] args) {
    }
}
