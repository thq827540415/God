package com.shadow.garden.basic.geek.day01;

/**
 * 快慢指针解决 leetcode 141
 */
public class E07_LinkedListCycle {

    public static boolean hasCycle(Node head) {
        Node fast = head;
        while (fast != null && fast.next != null) {
            fast = fast.next.next;
            head = head.next;
            if (head == fast) return true;
        }
        return false;
    }

    public static void main(String[] args) {
    }
}
