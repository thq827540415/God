package com.solitude.basic.geek.day01;

/**
 * @Author lancer
 * @Date 2022/8/16 20:27
 * @Description 反转链表 leetcode: 206
 */
public class E05_ReverseLinkedList {

    private static class Node {
        int value;
        Node next;

        public Node() {
        }

        public Node(int value, Node next) {
            this.value = value;
            this.next = next;
        }
    }

    public static void main(String[] args) {
        Node head = new Node(1, null);
        Node p = head;
        for (int i = 2; i <= 5; i++) {
            Node temp = new Node(i, null);
            p.next = temp;
            p = temp;
        }
        reverse(head);
    }

    private static Node reverse(Node head) {
        Node last = null;
        while (head != null) {
            Node nextHead = head.next;
            head.next = last;
            last = head;
            head = nextHead;
        }
        return last;
    }
}
