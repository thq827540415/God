package com.ivi.algorithm.geek.day01;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// Leetcode 146
public class E14_LRUCache {
    /**
     * 使用自带的集合
     */
    static class InnerCollection extends LinkedHashMap<Integer, Integer> {
        private final Integer capacity;

        public InnerCollection(int capacity) {
            super(capacity, 0.75F, true);
            this.capacity = capacity;
        }

        public int get(int key) {
            return super.getOrDefault(key, -1);
        }

        public void put(int key, int value) {
            super.put(key, value);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, Integer> eldest) {
            return size() > capacity;
        }
    }

    /**
     * 使用哈希表 + 双向链表
     */
    static class MyLRUCache {
        static class DLinkedList {
            int key;
            int val;
            DLinkedList prev, next;

            public DLinkedList() {
            }

            public DLinkedList(int key, int val) {
                this.key = key;
                this.val = val;
            }
        }

        private int size;
        private final int capacity;
        private final DLinkedList head, tail;
        private final Map<Integer, DLinkedList> cache = new HashMap<>();

        public MyLRUCache(int capacity) {
            this.capacity = capacity;
            head = new DLinkedList();
            tail = new DLinkedList();
            head.next = tail;
            tail.prev = head;
        }

        public int get(int key) {
            DLinkedList node = cache.get(key);
            if (node == null) {
                return -1;
            }
            move2Head(node);
            return node.val;
        }

        public void put(int key, int val) {
            DLinkedList node = cache.get(key);
            if (node == null) {
                DLinkedList newNode = new DLinkedList(key, val);
                size++;
                add2Head(newNode);
                cache.put(key, newNode);
                if (size > capacity) {
                    DLinkedList t = removeTail();
                    cache.remove(t.key);
                    size--;
                }
            } else {
                node.val = val;
                move2Head(node);
            }
        }

        void move2Head(DLinkedList node) {
            removeNode(node);
            add2Head(node);
        }

        void removeNode(DLinkedList node) {
            node.next.prev = node.prev;
            node.prev.next = node.next;
        }

        void add2Head(DLinkedList node) {
            node.next = head.next;
            head.next = node;
            node.next.prev = node;
            node.prev = head;
        }

        DLinkedList removeTail() {
            DLinkedList t = tail.prev;
            removeNode(t);
            return t;
        }
    }

    public static void main(String[] args) {
    }
}
