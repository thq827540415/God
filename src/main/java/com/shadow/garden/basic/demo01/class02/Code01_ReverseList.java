package com.shadow.garden.basic.demo01.class02;

/**
 * @Author lancer
 * @Date 2022/1/1 2:54 下午
 * 反转链表
 */
public class Code01_ReverseList {

    public static class Node {
        public int value;
        public Node next;

        public Node(int data) {
            value = data;
        }
    }

    /**
     * 双向链表
     */
    public static class DoubleNode {
        public int value;
        public DoubleNode last;
        public DoubleNode next;

        public DoubleNode(int data) {
            value = data;
        }
    }

    /**
     * 反转单向链表
     *
     * @param head
     * @return
     */
    public static Node reverseLinkedList(Node head) {
        Node pre = null;
        while (head != null) {
            Node next = head.next;
            head.next = pre;
            pre = head;
            head = next;
        }
        return pre;
    }

    /**
     * 反转双向链表
     *
     * @param head
     * @return
     */
    public static DoubleNode reverseDoubleList(DoubleNode head) {
        DoubleNode pre = null;
        while (head != null) {
            DoubleNode next = head.next;
            head.next = pre;
            head.last = next;
            pre = head;
            head = next;
        }
        return pre;
    }

    /**
     * 反转某段单链表
     */
    public Node reverseBetween(Node head, int left, int right) {
        // 重点是summaryNode，避免从第一个节点就开始反转的情况
        Node summaryNode = new Node(-1);
        summaryNode.next = head;
        Node start = summaryNode;
        Node end = summaryNode;
        // 记住头、尾
        // 传入需要反转的地方
        for (int i = 1; i <= left - 1; i++) {
            start = start.next;
        }
        for (int i = 1; i <= right + 1; i++) {
            end = end.next;
        }

        // 进行反转
        Node pre = end;
        Node curr = start.next;
        while (curr != end) {
            Node next = curr.next;
            curr.next = pre;
            pre = curr;
            curr = next;
        }
        start.next = pre;
        return summaryNode.next;
    }


    /**
     * 判断两个无环单链表是否相等
     *
     * @param head1
     * @param head2
     * @return
     */
    public static boolean checkLinkedListEqual(Node head1, Node head2) {
        while (head1 != null && head2 != null) {
            if (head1.value != head2.value) {
                return false;
            }
            head1 = head1.next;
            head2 = head2.next;
        }
        return head1 == null && head2 == null;
    }
}
