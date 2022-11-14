package com.lancer.algorithm.datastruct;


import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.nio.charset.Charset;

/**
 * 使用Google Guava包提供的BloomFilter
 * 过滤器中的bitmap是由m位二进制组成，包含k个哈希函数，此次操作前已经插入了n次，则
 * (1 - (1 - 1 / m) ^ (k * n)) ^ k (m -> oo) = (1 - e ^ (-k * n / m)) ^ k
 * 由上式可知，在m比较大时，误判率大致与n成正比，与k、m成反比
 */
public class E02_BloomFilter {
    public static void main(String[] args) {
        BloomFilter<CharSequence> bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(Charset.defaultCharset()),
                // 预期放入bitmap中的数据个数
                400,
                // 默认值为3%
                0.0001);

        bloomFilter.put("abc");

        System.out.println(bloomFilter.mightContain("abc"));
        System.out.println(bloomFilter.mightContain("def"));
    }
}
