package com.solitude.basic.demo01.class02;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

/**
 * @Author lancer
 * @Date 2022/1/2 11:57 下午
 * @Description
 */
public class Main {

    private static class TwoStackImplementOneQueue {
        private final Stack<Integer> stackOffer;
        private final Stack<Integer> stackPoll;

        public TwoStackImplementOneQueue() {
            this.stackOffer = new Stack<>();
            this.stackPoll = new Stack<>();
        }

        public void offerToPoll() {
            if (stackPoll.isEmpty()) {
                while (!stackOffer.isEmpty()) {
                    stackPoll.push(stackOffer.pop());
                }
            }
        }

        public boolean empty() {
            offerToPoll();
            return stackPoll.isEmpty();
        }

        public void offer(int value) {
            stackOffer.push(value);
        }

        public int poll() {
            if (!empty()) {
                return stackPoll.pop();
            }
            return -1;
        }

        public int peek() {
            if (!empty()) {
                return stackPoll.peek();
            }
            return -1;
        }

        public static void main(String[] args) {
            TwoStackImplementOneQueue queue = new TwoStackImplementOneQueue();
            queue.offer(1);
            queue.offer(2);
            queue.offer(3);
            System.out.println(queue.peek());
            queue.poll();
            System.out.println(queue.peek());
            queue.poll();
            System.out.println(queue.peek());
        }
    }


    private static class TwoQueueImplementOneStack {
        private Queue<Integer> queue;
        private Queue<Integer> help;


        public TwoQueueImplementOneStack() {
            this.queue = new LinkedList<>();
            this.help = new LinkedList<>();
        }

        public void push(int value) {
            help.offer(value);
            while (!empty()) {
                help.offer(queue.poll());
            }
            swapTwoQueue();
        }

        public Integer pop() {
            return queue.poll();
        }

        public Integer peek() {
            return queue.peek();
        }

        public void swapTwoQueue() {
            Queue<Integer> temp = this.queue;
            this.queue = this.help;
            this.help = temp;
        }

        public boolean empty() {
            return queue.isEmpty();
        }

        public static void main(String[] args) {
            TwoQueueImplementOneStack stack = new TwoQueueImplementOneStack();
            stack.push(2);
            stack.push(3);
            stack.push(4);
            System.out.println(stack.peek());
            stack.pop();
            System.out.println(stack.peek());
            stack.pop();
            System.out.println(stack.peek());
        }
    }
}
