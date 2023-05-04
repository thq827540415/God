package com.ivi.algorithm.geek.day01;

import com.ivi.datastruct.ListNode;

/**
 * @Author lancer
 * @Date 2022/8/16 21:07
 * @Description k个一组翻转链表 leetcode：25
 */
public class E12_ReverseNodesInKGroup {
    public static void main(String[] args) {
        ListNode<Integer> head = new ListNode<>(1);
        ListNode<Integer> p = head;
        for (int i = 2; i <= 5; i++) {
            p.next = new ListNode<>(i);
            p = p.next;
        }
        // LinkedNode<Integer>  node = reverseKGroup1(head, 2);
        // print(head);

        ListNode<Integer> node = process(head, 2);
        print(node);
    }

    private static void print(ListNode<Integer> head) {
        System.out.println();
        while (head != null) {
            System.out.print(head.val + " ");
            head = head.next;
        }
    }

    /**
     * 先处理内部的边，再处理边界的两条边
     */
    private static ListNode<Integer> process(ListNode<Integer> head, int k) {
        ListNode<Integer> protect = new ListNode<>(-1, head);
        // 记录上一组的end
        ListNode<Integer> last = protect;
        while (head != null) {
            ListNode<Integer> end = getEnd(head, k);
            if (end == null) break;

            ListNode<Integer> nextGroupHead = end.next;
            // 处理head到end之间的k - 1条边的反转
            doReverse(head, end);
            // 上一组end跟本组的新开始（end）建立联系
            last.next = end;
            // 本组的新结尾（head）跟下一组建立关系
            head.next = nextGroupHead;
            // 本组的新结尾（head）作为下一组的 -> 上一组end
            last = head;
            // 跳转到下一组
            head = nextGroupHead;
        }
        return protect.next;
    }

    private static ListNode<Integer> getEnd(ListNode<Integer> head, int k) {
        while (head != null) {
            k--;
            if (k == 0) break;
            head = head.next;
        }
        return head;
    }

    private static void doReverse(ListNode<Integer> start, ListNode<Integer> end) {
        if (start == end) return;
        ListNode<Integer> pre = start;
        start = start.next;
        while (start != end) {
            ListNode<Integer> nextHead = start.next;
            start.next = pre;
            pre = start;
            start = nextHead;
        }
        end.next = pre;
    }

    // ======================================== normal solution ========================================
    private static ListNode<Integer> reverseKGroup(ListNode<Integer> head, int k) {
        ListNode<Integer> first = head;
        ListNode<Integer> last = first;
        int groupNum = 1;
        int count = 0;
        ListNode<Integer> start = last;
        ListNode<Integer> end;
        while (head != null) {
            end = head;
            head = head.next;
            if (++count == k) {
                start = reverse(start, end);
                count = 0;
                if (groupNum++ == 1) {
                    first = end;
                } else {
                    ListNode<Integer> preLast = last.next;
                    last.next = end;
                    last = preLast;
                }
            }
        }
        return first;
    }

    /**
     * end != null
     */
    private static ListNode<Integer> reverse(ListNode<Integer> start, ListNode<Integer> end) {
        ListNode<Integer> endNext = end.next;
        ListNode<Integer> pre = endNext;
        while (start != endNext) {
            ListNode<Integer> curr = start;
            start = start.next;
            curr.next = pre;
            pre = curr;
        }
        return start;
    }
}
