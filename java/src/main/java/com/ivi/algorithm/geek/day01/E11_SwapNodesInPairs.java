package com.ivi.algorithm.geek.day01;

import com.ivi.datastruct.ListNode;

// Leetcode 24
public class E11_SwapNodesInPairs {

    // 交换两个相邻节点，不交换内部值
    public static ListNode<Integer> swapPairs(ListNode<Integer> head) {
        if (head == null || head.next == null) {
            return head;
        }
        ListNode<Integer> protect = new ListNode<>(-1, head);
        ListNode<Integer> prev = protect;
        ListNode<Integer> curr = head;
        ListNode<Integer> remember;

        while (curr != null && curr.next != null) {
            remember = curr.next.next;
            curr.next.next = curr;
            prev.next = curr.next;
            curr.next = remember;
            prev = curr;
            curr = prev.next;
        }

        return protect.next;
    }

    public static void main(String[] args) {
        ListNode<Integer> head = ListNode.generate(new Integer[]{1, 2, 3, 4});
        // 1, 2, 3, 4
        ListNode.print(head);

        ListNode<Integer> result = swapPairs(head);

        // 2, 1, 4, 3
        ListNode.print(result);
    }
}
