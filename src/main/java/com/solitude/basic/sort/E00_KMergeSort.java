package com.solitude.basic.sort;


import java.io.IOException;
import java.util.List;
import java.util.PriorityQueue;

/**
 * 多路归并排序
 * 1. 现在有K个文件，其中第i个文件内部存储有Ni个正整数（这些整数在文件内按照从小到大的顺序存储）
 * 2. 对每个文件设计一个指针，取出K个指针中数值最小的一个，然后把最小的按个指针后移，
 *  接着继续找K个指针中数值最小的一个，直到N个文件全部读完为止。
 * 3. 首先用一个最小堆来维护K个指针，每次从堆中取最小值，开销为logK，最多从堆中取N1 + N2 + ... + Nk次元素
 */
public class E00_KMergeSort {
    private interface FileReader {
        boolean hasNext() throws IOException;
        int next() throws IOException;
    }

    private interface FileWriter {
        void append(int value) throws IOException;
    }

    private static class Pair<IN, OUT> {
        private final IN key;
        private final OUT value;

        public Pair(IN reader, OUT next) {
            this.key = reader;
            this.value = next;
        }

        public OUT getValue() {
            return value;
        }

        public IN getKey() {
            return key;
        }
    }

    public void kMergerSort(final List<FileReader> readers, FileWriter writer) throws IOException {
        PriorityQueue<Pair<FileReader, Integer>> heap =
                new PriorityQueue<>((p1, p2) -> p1.getValue() - p2.getValue());
        for (FileReader reader : readers) {
            if (reader.hasNext()) {
                heap.add(new Pair<>(reader, reader.next()));
            }
        }
        while (!heap.isEmpty()) {
            Pair<FileReader, Integer> current = heap.poll();
            writer.append(current.getValue());
            FileReader currentReader = current.getKey();
            if (currentReader.hasNext()) {
                heap.add(new Pair<>(currentReader, currentReader.next()));
            }
        }
    }

    public static void main(String[] args) {
    }
}
