package com.ivi.algorithm.geek.day02;

// Leetcode 155
public class E02_MinStack {
    // 能在O(1)时间内检索到最小元素的栈
    // 使用两个栈，一个用来保存数据，另一个用来保存最小元素
    static class MinStack {
        int[] stack;
        int[] minStack;

        int top = -1;

        public MinStack() {
            stack = new int[30000];
            minStack = new int[30000];
        }

        public void push(int val) {
            stack[++top] = val;
            minStack[top] = top == 0 ? val : Math.min(minStack[top - 1], val);
        }

        public void pop() {
            top--;
        }

        public int top() {
            return stack[top];
        }

        public int getMin() {
            return minStack[top];
        }
    }


    // 使用单链表完成，这个牛
    static class MinStackWithLinkedList {
        static class Node {
            int val;
            int min;
            Node next;
            public Node(int val) {
                this.val = val;
            }
        }

        Node head;

        public MinStackWithLinkedList() {
        }

        public void push(int val) {
            Node node = new Node(val);
            node.min = head == null ? val : Math.min(head.min, val);
            node.next = head;
            head = node;
        }

        public void pop() {
            head = head.next;
        }

        public int top() {
            return head.val;
        }

        public int getMin() {
            return head.min;
        }
    }

    public static void main(String[] args) {
        MinStackWithLinkedList stack = new MinStackWithLinkedList();
        stack.push(-2);
        stack.push(0);
        stack.push(-3);
        System.out.println(stack.getMin());
        stack.pop();
        System.out.println(stack.top());
        System.out.println(stack.getMin());
    }
}
