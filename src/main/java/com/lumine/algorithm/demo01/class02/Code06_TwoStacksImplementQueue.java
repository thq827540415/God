package com.lumine.algorithm.demo01.class02;

import java.util.Stack;

/**
 * @Author lancer
 * @Date 2022/1/1 11:06 下午
 * @Description 使用栈结构实现队列结构
 */

public class Code06_TwoStacksImplementQueue {

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

        // push栈向pop栈倒入数据
        // pop栈必须为空，push栈必须全部倒完
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
