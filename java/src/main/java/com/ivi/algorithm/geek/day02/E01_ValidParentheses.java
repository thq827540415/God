package com.ivi.algorithm.geek.day02;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;

// Leetcode 20
public class E01_ValidParentheses {
    // 使用语言自带的数据结构
    public static boolean isValid(String s) {
        if (s.length() % 2 == 1) {
            return false;
        }

        HashMap<Character, Character> pairs = new HashMap<Character, Character>() {
            {
                put(')', '(');
                put(']', '[');
                put('}', '{');
            }
        };

        // Vector线程安全，效率太低，且可以随机位置访问，不符合栈思想
        // Stack<Character> stack = new Stack<>();

        // Deque具备队列的FIFO，也具备栈的LIFO
        // 如果多访问，实现采用ArrayDeque
        Deque<Character> stack = new LinkedList<>();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (pairs.containsKey(ch)) {
                // 如果是右括号
                if (stack.isEmpty() || stack.peek() != pairs.get(ch)) {
                    return false;
                }
                stack.pop();
            } else {
                // 左括号
                stack.push(ch);
            }
        }
        return stack.isEmpty();
    }

    // 自己写一个栈
    public static boolean valid(String s) {
        if (s.length() % 2 == 1) {
            return false;
        }
        char[] stack = new char[s.length() / 2 + 1];
        int top = -1;

        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '(' || ch == '[' || ch == '{') {
                stack[++top] = ch;
            } else {
                // ')'、']'、'}'
                if (top == -1 || (stack[top] != choose(ch))) {
                    return false;
                }
                top--;
            }
        }
        return top == -1;
    }

    static char choose(char ch) {
        char res = 0;
        switch (ch) {
            case ')':
                res = '(';
                break;
            case ']':
                res = '[';
                break;
            case '}':
                res = '{';
            default:
        }
        return res;
    }

    public static void main(String[] args) {
        // ([)] -> false、([]) -> true
        System.out.println(valid("([)]"));
        System.out.println(valid("([])"));
        System.out.println(valid("()[]"));
    }
}
