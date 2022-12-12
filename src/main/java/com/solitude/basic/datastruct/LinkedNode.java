package com.solitude.basic.datastruct;

public class LinkedNode<T> {
    public T val;
    public LinkedNode<T> next;

    public LinkedNode() {
    }

    public LinkedNode(T val) {
        this.val = val;
    }

    public static <T> LinkedNode<T> generate(T[] arr) {
        LinkedNode<T> head = new LinkedNode<>();
        LinkedNode<T> p = head;
        for (T val : arr) {
            p.next = new LinkedNode<>(val);
            p = p.next;
        }
        return head.next;
    }

    public static void print(LinkedNode<?> head) {
        if (head == null) return;
        while (head.next != null) {
            System.out.print(head.val + " -> ");
            head = head.next;
        }
        System.out.println(head.val + " -> null");
    }

    public static <T> void swap(LinkedNode<T> n1, LinkedNode<T> n2) {
        T tmp = n1.val;
        n1.val = n2.val;
        n2.val = tmp;
    }
}
