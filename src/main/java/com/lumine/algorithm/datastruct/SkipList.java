package com.lumine.algorithm.datastruct;

import java.util.*;

public class SkipList {

    private static class Node {
        public int value;
        public Node right, down;

        private Node(int value) {
            this.value = value;
            this.right = null;
            this.down = null;
        }

        private Node(int value, Node right, Node down) {
            this.value = value;
            this.right = right;
            this.down = down;
        }

        public static Node of(int value) {
            return new Node(value);
        }

        public static Node of(int value, Node right, Node down) {
            return new Node(value, right, down);
        }
    }

    private final List<Node> head;

    private final double p;

    private int size;

    private final Random r;


    private SkipList() {
        this(0.5, 6);
    }

    private SkipList(double p, int maxHeight) {
        this.head = new ArrayList<>();
        this.p = p;
        this.size = 0;
        assert maxHeight > 0;
        for (int i = 0; i < maxHeight; i++) {
            head.add(Node.of(0));
            if (i != 0) {
                head.get(i).down = head.get(i - 1);
            }
        }
        r = new Random();
    }

    private void add(int value) {
        int maxHeight = head.size();
        Node[] update = new Node[maxHeight];

        // from top
        int height = maxHeight - 1;
        Node it = head.get(height);
        while (Objects.nonNull(it)) {
            if (Objects.isNull(it.right) || value < it.right.value) {
                update[height--] = it;
                it = it.down;
            } else {
                it = it.right;
            }
        }
        int currHeight = randomHeight();
        Node[] curr = new Node[currHeight];
        for (height = 0; height < currHeight; height++) {
            curr[height] = Node.of(value);
            if (height > 0) {
                curr[height].down = curr[height - 1];
            }
        }

        // 增高层数，形成0 -> current[height] -> null
        if (maxHeight < currHeight) {
            for (height = maxHeight; height < currHeight; height++) {
                head.add(Node.of(0, head.get(height - 1), curr[height]));
            }
        }

        // 从update[]连接curr[]
        for (height = 0; height < Math.min(maxHeight, currHeight); height++) {
            curr[height].right = update[height].right;
            update[height].right = curr[height];
        }
        size++;
    }

    private boolean find(int value) {
        int maxHeight = head.size();
        Node it = head.get(maxHeight - 1);
        while (Objects.nonNull(it)) {
            if (Objects.isNull(it.right) || value <= it.right.value) {
                if (Objects.nonNull(it.right) && it.right.value == value) {
                    return true;
                }
                it = it.down;
            } else {
                it = it.right;
            }
        }
        return false;
    }

    private boolean remove(int value) {
        boolean isFound = find(value);
        // do the removing
        if (isFound) {
            for (int i = head.size() - 1; i >= 1; i--) {
                if (Objects.isNull(head.get(i).right)) {
                    head.remove(i);
                }
            }
            size--;
        }
        return isFound;
    }

    private int randomHeight() {
        int height = 0;
        while (r.nextDouble() < p && height < 64) {
            height++;
        }
        return height + 1;
    }

    private int[] toArray() {
        int i = 0;
        int[] values = new int[size];
        for (Node it = head.get(0).right; Objects.nonNull(it); it = it.right) {
            values[i++] = it.value;
        }
        return values;
    }

    private void print() {
        for (int i = head.size() - 1; i >= 0; i--) {
            Node it = head.get(i).right;
            System.out.print("Height #" + i + ": ");
            while (Objects.nonNull(it)) {
                System.out.print(it.value + " ");
                it = it.right;
            }
            System.out.println();
        }
    }

    private int size() {
        return size;
    }

    public static void main(String[] args) {
        SkipList skipList = new SkipList();
        int[] values = new int[]{23, 12, 33, 45, 8, 1, 48, 49, 32, 24, 0, 88};
        int[] result = new int[]{0, 1, 8, 12, 23, 24, 32, 33, 45, 48, 49, 88};

        for (int value : values) {
            skipList.add(value);
        }

        skipList.print();
        assert skipList.size() == result.length;
        assert Arrays.equals(result, skipList.toArray());
    }
}
