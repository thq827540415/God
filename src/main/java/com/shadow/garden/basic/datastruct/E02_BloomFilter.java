package com.shadow.garden.basic.datastruct;


import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.apache.hadoop.hbase.util.Bytes;

/**
 * 使用Google Guava包提供的BloomFilter
 * 过滤器中的bitmap是由m位二进制组成，包含k个哈希函数，此次操作前已经插入了n次，则
 * (1 - (1 - 1 / m) ^ (k * n)) ^ k (m -> oo) = (1 - e ^ (-k * n / m)) ^ k
 * 由上式可知，在m比较大时，误判率大致与n成正比，与k、m成反比
 */
public class E02_BloomFilter {
    /**
     * hash函数的个数
     */
    private final int k;
    /**
     * 表示每个Key占用的二进制bit数，若有x个Key，则N = x * bitsPerKey
     */
    private final int bitsPerKey;
    private int bitLen;
    private byte[] result;

    public E02_BloomFilter(int k, int bitsPerKey) {
        this.k = k;
        this.bitsPerKey = bitsPerKey;
    }

    public byte[] generate(byte[][] keys) {
        assert keys != null;
        bitLen = keys.length * bitsPerKey;
        // align the bitLen
        bitLen = ((bitLen + 7) / 8) << 3;
        bitLen = Math.max(bitLen, 64);
        // each byte have 8 bit.
        result = new byte[bitLen >> 3];
        for (byte[] key : keys) {
            assert key != null;

            int h = Bytes.hashCode(key);
            // 做k次hash时，可借助h位运算来实现多次hash，性能会比较好
            for (int t = 0; t < k; t++) {
                int idx = (h % bitLen + bitLen) % bitLen;
                result[idx / 8] |= (1 << (idx % 8));
                int delta = (h >> 17) | (h << 15);
                h += delta;
            }
        }
        return result;
    }

    public boolean mightContain(byte[] key) {
        assert result != null;
        int h = Bytes.hashCode(key);
        // hash k times
        for (int t = 0; t < k; t++) {
            int idx = (h % bitLen + bitLen) % bitLen;
            if ((result[idx / 8] & (1 << (idx % 8))) == 0) {
                return false;
            }
            int delta = (h >> 17) | (h << 15);
            h += delta;
        }
        return true;
    }

    public static void main(String[] args) {
        BloomFilter<CharSequence> bloomFilter = BloomFilter.create(
                // Funnels.stringFunnel(Charset.defaultCharset()),
                Funnels.stringFunnel(),
                // 预期放入bitmap中的数据个数
                400,
                // 默认值为3%
                0.0001);

        bloomFilter.put("abc");

        System.out.println(bloomFilter.mightContain("abc"));
        System.out.println(bloomFilter.mightContain("def"));
    }
}
