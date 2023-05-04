package com.ivi.grammar;

import java.util.function.Function;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * @Author lancer
 * @Date 2023/3/13 16:50
 * @Description
 */
public class Streams {
    static long n = 10_000_000L;

    public static void main(String[] args) {
        compute(Streams::parallelRangedSum, n);
        compute(Streams::sequentialSum, n);
        compute(Streams::iterativeSum, n);
        compute(Streams::parallelSum, n);
    }

    static long sequentialSum(long n) {
        return Stream.iterate(1L, i -> i + 1)
                .limit(n)
                .reduce(0L, Long::sum);
    }


    static long iterativeSum(long n) {
        long res = 0L;
        for (int i = 0; i < n; i++) {
            res += i;
        }
        return res;
    }


    static long parallelSum(long n) {
        return Stream.iterate(1L, i -> i + 1)
                .limit(n)
                .parallel()
                .reduce(0L, Long::sum);
    }

    static long parallelRangedSum(long n) {
        return LongStream.rangeClosed(1, n)
                .parallel()
                .reduce(0L, Long::sum);
    }


    static void compute(Function<Long, Long> fn, long n) {
        long fastest = Long.MAX_VALUE;
        for (int i = 0; i < 5; i++) {
            long start = System.nanoTime();
            fn.apply(n);
            long duration = (System.nanoTime() - start) / 1_000_000;

            if (duration < fastest) fastest = duration;
        }
        System.out.println(fastest);
    }
}
