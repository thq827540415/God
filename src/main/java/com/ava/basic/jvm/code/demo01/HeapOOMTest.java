package com.ava.basic.jvm.code.demo01;

import java.util.ArrayList;
import java.util.Random;

/**
 * @Author lancer
 * @Date 2022/4/5 13:29
 * @Description -Xms600m -Xmx600m -XX:NewRatio=3 -X
 */
public class HeapOOMTest {

    private static class Picture {
        private byte[] pixels;

        public Picture(int length) {
            this.pixels = new byte[length];
        }
    }

    public static void main(String[] args) {
        ArrayList<Picture> list = new ArrayList<>();
        while (true) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            list.add(new Picture(new Random().nextInt(1024 * 1024)));
        }
    }
}
