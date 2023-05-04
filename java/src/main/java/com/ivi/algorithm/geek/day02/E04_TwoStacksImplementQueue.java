package com.ivi.algorithm.geek.day02;

import java.util.Stack;

// Leetcode 232
public class E04_TwoStacksImplementQueue {

    /**
     * 自定义队列默认保存Integer类型数据
     */
    public static class TwoStacksQueue {
        public final Stack<Integer> stackOffer;
        public final Stack<Integer> stackPoll;

        public TwoStacksQueue() {
            stackOffer = new Stack<>();
            stackPoll = new Stack<>();
        }

        // 用于输出的栈为空时，才进行数据的交换
        private void offerToPoll() {
            if (this.stackPoll.empty()) {
                while (!this.stackOffer.empty()) {
                    this.stackPoll.push(this.stackOffer.pop());
                }
            }
        }

        public boolean empty() {
            offerToPoll();
            return this.stackPoll.isEmpty();
        }

        public void offer(int pushInt) {
            this.stackOffer.push(pushInt);
        }

        public int poll() {
            if (empty()) {
                throw new RuntimeException("Queue is empty!");
            }
            return this.stackPoll.pop();
        }

        public int peek() {
            if (empty()) {
                throw new RuntimeException("Queue is empty!");
            }
            return this.stackPoll.peek();
        }
    }

    public static void main(String[] args) {
        TwoStacksQueue test = new TwoStacksQueue();
        test.offer(1);
        test.offer(2);
        test.offer(3);
        System.out.println("peek:" + test.peek());
        System.out.println("poll:" + test.poll());
        System.out.println("peek:" + test.peek());
        System.out.println("poll:" + test.poll());
        System.out.println("peek:" + test.peek());
        System.out.println("poll:" + test.poll());
        System.out.println("peek:" + test.peek());
    }

}
