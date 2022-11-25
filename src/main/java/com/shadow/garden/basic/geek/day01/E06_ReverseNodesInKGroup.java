package com.shadow.garden.basic.geek.day01;

/**
 * @Author lancer
 * @Date 2022/8/16 21:07
 * @Description k个一组翻转链表 leetcode：25
 */
public class E06_ReverseNodesInKGroup {

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
            p.next = new Node(i, null);
            p = p.next;
        }
        // Node node = reverseKGroup1(head, 2);
        // print(head);

        Node node = process(head, 2);
        print(node);
    }

    private static void print(Node head) {
        System.out.println();
        while (head != null) {
            System.out.print(head.value + " ");
            head = head.next;
        }
    }

    /**
     * 先处理内部的边，再处理边界的两条边
     */
    private static Node process(Node head, int k) {
        Node protect = new Node(-1, head);
        // 记录上一组的end
        Node last = protect;
        while (head != null) {
            Node end = getEnd(head, k);
            if (end == null) break;

            Node nextGroupHead = end.next;
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

    private static Node getEnd(Node head, int k) {
        while (head != null) {
            k--;
            if (k == 0) break;
            head = head.next;
        }
        return head;
    }

    private static void doReverse(Node start, Node end) {
        if (start == end) return;
        Node pre = start;
        start = start.next;
        while (start != end) {
            Node nextHead = start.next;
            start.next = pre;
            pre = start;
            start = nextHead;
        }
        end.next = pre;
    }

    // ======================================== normal solution ========================================
    private static Node reverseKGroup(Node head, int k) {
        Node first = head;
        Node last = first;
        int groupNum = 1;
        int count = 0;
        Node start = last;
        Node end;
        while (head != null) {
            end = head;
            head = head.next;
            if (++count == k) {
                start = reverse(start, end);
                count = 0;
                if (groupNum++ == 1) {
                    first = end;
                } else {
                    Node preLast = last.next;
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
    private static Node reverse(Node start, Node end) {
        Node endNext = end.next;
        Node pre = endNext;
        while (start != endNext) {
            Node curr = start;
            start = start.next;
            curr.next = pre;
            pre = curr;
        }
        return start;
    }
}
