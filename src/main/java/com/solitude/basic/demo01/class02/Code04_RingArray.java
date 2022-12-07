package com.solitude.basic.demo01.class02;

/**
 * @Author lancer
 * @Date 2022/1/1 11:02 下午
 * @Description 用数组实现栈
 */
public class Code04_RingArray {

    public static class MyQueue {
        private int[] arr;
        private int pushi;
        private int polli;
        private int size;
        private final int limit;

        public MyQueue(int l) {
            arr = new int[l];
            pushi = 0;
            polli = 0;
            size = 0;
            limit = l;
        }

        public void offer(int value) {
            if (isFull()) {
                throw new RuntimeException("队列满了，不能再加了");
            }
            size++;
            arr[pushi] = value;
            pushi = nextIndex(pushi);
        }

        /**
         * 从队列
         * @return
         */
        public int poll() {
            if (isEmpty()) {
                throw new RuntimeException("队列空了，不能再拿了");
            }
            size--;
            int ans = arr[polli];
            polli = nextIndex(polli);
            return ans;
        }

        /**
         * 判断队列是否空
         * @return
         */
        public boolean isEmpty() {
            return size == 0;
        }

        /**
         * 判断队列是否满
         * @return
         */
        public boolean isFull() {
            return size == limit;
        }

        /**
         * 获取下个位置的下标
         *
         * @param i
         * @return
         */
        private int nextIndex(int i) {
            return i < limit - 1 ? i + 1 : 0;
        }
    }
}
