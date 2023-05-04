package com.ivi.basic.algorithm.demo01.class02;

import java.util.LinkedList;
import java.util.Queue;

/**
 * @Author lancer
 * @Date 2022/1/2 5:34 下午
 * @Description 使用两个队列实现栈
 */

public class Code07_TwoQueueImplementStack {

    /**
     * 通过每次只取最后一个元素，实现栈
     */
    private static class TwoQueueStack1<T> {
        private Queue<T> queue;
        // help的作用，用于保存除栈顶元素之外的队列
        private Queue<T> help;

        public TwoQueueStack1() {
            queue = new LinkedList<>();
            help = new LinkedList<>();
        }

        // offer进队列
        public void push(T value) {
            // 直接将元素添加到LinkedList的尾部
            queue.offer(value);
        }

        /**
         * 由于peek只能获取最开始元素的值，所以需要两个queue
         *
         * @return
         */
        public T pop() {
            // 将数据按照进栈的顺序，添加到help队列中，queue中只保留需要出栈的元素
            while (queue.size() > 1) {
                help.offer(queue.poll());
            }
            // 直接从LinkedList删除第一个元素
            T ans = queue.poll();

            swapTwoQueue();
            return ans;
        }

        public T peek() {
            // 先获取到栈顶元素
            while (queue.size() > 1) {
                help.offer(queue.poll());
            }
            T ans = queue.peek();

            // 将栈顶元素重新放回栈中
            help.offer(queue.poll());

            swapTwoQueue();
            return ans;
        }

        public void swapTwoQueue() {
            Queue<T> temp = this.queue;
            this.queue = this.help;
            this.help = temp;
        }

        public boolean empty() {
            return queue.isEmpty();
        }

    }

    /**
     * 两个队列通过实现前插法，实现栈
     *
     * @param <T>
     */
    private static class TwoQueueStack2<T> {
        private Queue<T> queue;
        private Queue<T> help;

        public TwoQueueStack2() {
            this.queue = new LinkedList<>();
            this.help = new LinkedList<>();
        }

        public void push(T value) {
            help.offer(value);
            // 将入栈的元素，使用前插存放
            while (!empty()) {
                help.offer(queue.poll());
            }
            swapTwoQueue();
        }

        public T pop() {
            return queue.poll();
        }

        public T peek() {
            return queue.peek();
        }

        public void swapTwoQueue() {
            Queue<T> temp = this.queue;
            this.queue = this.help;
            this.help = temp;
        }

        public boolean empty() {
            return queue.isEmpty();
        }
    }

    public static void main(String[] args) {
        TwoQueueStack1<Integer> myStack1 = new TwoQueueStack1<>();
        myStack1.push(1);
        myStack1.push(2);
        myStack1.push(3);
        System.out.println(myStack1.peek());
        myStack1.pop();
        System.out.println(myStack1.peek());
        myStack1.pop();
        System.out.println(myStack1.peek());

        System.out.println("===================================");

        TwoQueueStack2<Integer> myStack2 = new TwoQueueStack2<>();
        myStack2.push(1);
        myStack2.push(2);
        myStack2.push(3);
        System.out.println(myStack2.peek());
        myStack2.pop();
        System.out.println(myStack2.peek());
        myStack2.pop();
        System.out.println(myStack2.peek());
    }
}
