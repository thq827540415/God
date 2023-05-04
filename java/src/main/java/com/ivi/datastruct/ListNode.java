package com.ivi.datastruct;

public class ListNode<T> {
    public T val;
    public ListNode<T> next;

    private ListNode() {
    }

    public ListNode(T val) {
        this(val, null);
    }

    public ListNode(T val, ListNode<T> next) {
        this.val = val;
        this.next = next;
    }

    public static <T> ListNode<T> generate(T[] arr) {
        ListNode<T> head = new ListNode<>();
        ListNode<T> p = head;
        for (T val : arr) {
            p.next = new ListNode<>(val);
            p = p.next;
        }
        return head.next;
    }

    public static void print(ListNode<?> head) {
        if (head == null) return;
        while (head.next != null) {
            System.out.print(head.val + " -> ");
            head = head.next;
        }
        System.out.println(head.val + " -> null");
    }

    public static <T> void swap(ListNode<T> n1, ListNode<T> n2) {
        T tmp = n1.val;
        n1.val = n2.val;
        n2.val = tmp;
    }
}
