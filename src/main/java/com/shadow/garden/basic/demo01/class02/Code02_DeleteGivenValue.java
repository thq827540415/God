package com.shadow.garden.basic.demo01.class02;

import java.util.HashSet;
import java.util.Set;

/**
 * @Author lancer
 * @Date 2022/1/1 9:02 下午
 * @Description 删除指定值
 */
public class Code02_DeleteGivenValue {

    public static class Node {
        public int value;
        public Node next;

        public Node(int value) {
            this.value = value;
        }

        public Node(int value, Node next) {
            this.value = value;
            this.next = next;
        }

    }

    /**
     * 删除指定的所有元素
     *
     * @param head
     * @param value
     * @return
     */
    public static Node removeElements(Node head, int value) {
        Node newHead = new Node(-1, head);
        Node pre = newHead;
        while (pre.next != null) {
            if (pre.next.value == value) {
                pre.next = pre.next.next;
            } else {
                pre = pre.next;
            }
        }
        return newHead.next;
    }

    /**
     * 删除重复的元素
     * [1,1,2,3] ==> [1,2,3]
     * @param head
     * @return
     */
    public Node removeDuplicateNodes(Node head) {
        Set<Integer> set = new HashSet<>();
        Node firstNode = new Node(-1, head);
        head = firstNode;
        while (head.next != null) {
            if (!set.add(head.next.value)) {
                // 重复了
                head.next = head.next.next;
            } else {
                head = head.next;
            }
        }
        return firstNode.next;
    }


    /**
     * 删除具有重复元素的所有节点
     * [1,1,2,3] ==> [2,3]
     * @param head
     * @return
     */
    public Node deleteDuplicates(Node head) {
        Node firstNode = new Node(-1, head);
        head = firstNode;
        while (head.next != null && head.next.next != null) {
            if (head.next.value == head.next.next.value) {
                int x = head.next.value;
                while (head.next != null && head.next.value == x) {
                    head.next = head.next.next;
                }
            } else {
                head = head.next;
            }
        }
        return firstNode.next;
    }

}
