package com.ivi.jvm.code.demo01;

/**
 * @Author lancer
 * @Date 2022/4/1 12:36 上午
 * @Description
 */
public class ByteCodeInterview02 {

    private static class Father {
        int x = 10;

        public Father() {
            this.print();
            x = 20;
        }

        public void print() {
            System.out.println("Father.x = " + x);
        }
    }

    private static class Son extends Father {
        int x = 30;

        public Son() {
            this.print();
            x = 40;
        }

        public void print() {
            System.out.println("Son.x = " + x);
        }
    }

    public static void main(String[] args) {
        /*
         * Son.x = 0
         * Son.x = 30
         * Son.x = 40
         * 20
         * */
        Father f = new Son();
        f.print();
        System.out.println(f.x);
    }
}