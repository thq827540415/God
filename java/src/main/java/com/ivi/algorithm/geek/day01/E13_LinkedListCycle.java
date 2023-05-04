package com.ivi.algorithm.geek.day01;

import com.ivi.datastruct.ListNode;

/**
 * 快慢指针解决环形链表 leetcode 141
 */
public class E13_LinkedListCycle {
    public static boolean hasCycle(ListNode<Integer> head) {
        ListNode<Integer> fast = head;
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
