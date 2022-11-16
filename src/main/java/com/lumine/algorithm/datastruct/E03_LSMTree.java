package com.lumine.algorithm.datastruct;

/**
 * 把多次写入合并成一次写入，从而减少磁盘寻道的开销
 * LSMTree索引由两部分组成：
 *  1. 内存部分使用SkipList来维护一个有序的KeyValue集合
 *  2. 磁盘部分有多个内部KeyValue有序的问价能组成
 */
public class E03_LSMTree {
}
