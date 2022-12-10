package com.solitude.basic.juc;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 功能和HashMap基本一致，内部使用红黑树实现
 * 特性：
 *  1. 迭代结果和存入顺序不一致
 *  2. key和value都不能为空
 *  3. 线程安全的
 *
 *  Concurrent类型的容器
 *      1. 内部很多操作使用CAS优化，一般可以提供较高吞吐量
 *      2. 弱一致性
 *          i. 遍历时弱一致性，例如，当利用迭代器遍历时，如果容器发生修改，迭代器仍可以继续进行遍历，这时内容是旧的
 *          ii. 求大小弱一致性，size操作未必是100%准确
 *          iii. 读取弱一致性
 */
public class MyConcurrentHashMap {

    static final String ALPHA = "abcdefghijklmnopqrstuvwxyz";

    // 单词计数
    public static void main(String[] args) {
        // genData();

        compute();
    }

    private static void compute() {
        // 有问题的方法
        demo(
                () -> new HashMap<String, Integer>(),
                (map, words) -> {
                    for (String word : words) {
                        Integer counter = map.get(word);
                        int newValue = counter == null ? 1 : counter + 1;
                        map.put(word, newValue);
                    }
                }
        );

        // 改进1
        demo(
                () -> new ConcurrentHashMap<String, LongAdder>(),
                (map, words) -> {
                    for (String word : words) {
                        map.computeIfAbsent(word, (key) -> new LongAdder()).increment();
                    }
                }
        );

        // 改进2
        demo(
                () -> new ConcurrentHashMap<String, Integer>(),
                (map, words) -> {
                    for (String word : words) {
                        // 无需原子变量
                        map.merge(word, 1, Integer::sum);
                    }
                }
        );
    }

    private static void genData() {
        int length = ALPHA.length();
        int count = 200;
        List<String> list = new ArrayList<>(length * count);

        for (int i = 0; i < length; i++) {
            char ch = ALPHA.charAt(i);
            for (int j  = 0; j < count; j++) {
                list.add(String.valueOf(ch));
            }
        }

        Collections.shuffle(list);

        for (int i = 0; i < 26; i++) {
            try (PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(
                            Files.newOutputStream(Paths.get("./output/out/" + (i + 1) + ".txt"))))) {
                String collect = String.join("\n", list.subList(i * count, (i + 1) * count));
                pw.print(collect);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static <V> void demo(Supplier<Map<String, V>> supplier,
                                 BiConsumer<Map<String, V>, List<String>> consumer) {
        Map<String, V> countMap = supplier.get();
        List<Thread> ts = new ArrayList<>();
        for (int i = 1; i <= 26; i++) {
            int idx = i;
            Thread thread = new Thread(() -> {
                List<String> words = readFromFile(idx);
                consumer.accept(countMap, words);
            });
            ts.add(thread);
        }

        ts.forEach(Thread::start);
        ts.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // 每个单词出现200次
        System.out.println(countMap);
    }

    public static List<String> readFromFile(int i) {
        ArrayList<String> words = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        Files.newInputStream(Paths.get("./output/out/" + i + ".txt"))))) {
            String word;
            while ((word = br.readLine()) != null) {
                words.add(word);
            }
            return words;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
