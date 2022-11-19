package com.shadow.garden.algorithm.datastruct;

import lombok.Cleanup;
import redis.clients.jedis.BitOP;
import redis.clients.jedis.Jedis;

import java.util.Arrays;
import java.util.BitSet;

public class E01_Bitmap {

    private static Bitmap bitmap;

    private static class Bitmap {
        public byte[] flags;
        public int size;

        private Bitmap(int size) {
            this.flags = new byte[size];
            this.size = size;
        }

        public static Bitmap of(int size) {
            return new Bitmap(size);
        }
    }

    private static void add(int value) {
        int address = value / 8;
        int offset = value % 8;
        if (address >= bitmap.size) {
            return;
        }
        bitmap.flags[address] |= 1 << offset;
    }

    private static boolean exists(int value) {
        int address = value / 8;
        int offset = value % 8;
        if (address >= bitmap.size) {
            return false;
        }
        return (bitmap.flags[address] & 1 << offset) != 0;
    }

    private static void useJavaBitSet() {
        BitSet bitSet = new BitSet(10);
        // 将指定下标设置为1
        bitSet.set(2, true);
        // 将某个范围设置为1
        bitSet.set(3, 5, true);

        System.out.println(bitSet.get(4));
    }

    private static void useJedis() {
        @Cleanup Jedis jedis = new Jedis("localhost", 6379);
        // 指定下标置为1
        jedis.setbit("bit", 2, true);
        // 获取指定下标的值
        System.out.println(jedis.getbit("bit", 0));
        // 获取范围内1的个数，不指定范围则获取全部个数
        System.out.println(jedis.bitcount("bit", 0, 5));
        // 获取第一个值为1的下标
        System.out.println(jedis.bitpos("bit", true));
        // 获取bit1 & bit2后的值, 并重新生成一个名为dest的bitmap
        System.out.println(jedis.bitop(BitOP.AND, "dest", "bit1", "bit2"));
    }

    public static void main(String[] args) {
        bitmap = Bitmap.of(400 / 8);

        // 12 -> 1...4          [0, 0, 0, 0, 1, 0, 0, 0] --> 8
        // 32 -> 4...0          [0, 0, 0, 0, 0, 0, 0, 1] --> 1

        Arrays
                .asList(12, 32)
                .forEach(E01_Bitmap::add);

        Arrays
                .asList(12, 48)
                .forEach(value -> System.out.println(exists(value)));
    }
}
