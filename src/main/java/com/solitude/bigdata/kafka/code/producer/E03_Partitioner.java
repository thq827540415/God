package com.solitude.bigdata.kafka.code.producer;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.utils.Utils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Partitioner
 * 1. 消息有可能需要经过拦截器Interceptor、序列化器Serializer和分区器Partitioner才能真正发往broker。
 * 2. 当ProducerRecord中没有指定partition字段，那么就要依赖分区器。
 * 3. 默认DefaultPartitioner中，如果key不为null，那么计算得到的分区号会是所有分区中的任意一个；如果key为null，那么计算得到的分区号仅为可用分区中的任意一个。
 */
public class E03_Partitioner {
    /**
     * 更改DefaultPartitioner
     */
    public static class MyParititioner implements Partitioner {

        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
            List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);
            int numPartitions = partitions.size();
            if (Objects.isNull(keyBytes)) {
                // 当key为null的值轮询发送到每个分区
                return counter.getAndIncrement() % numPartitions;
            } else {
                return Utils.toPositive(Utils.murmur2(keyBytes)) % numPartitions;
            }
        }

        @Override
        public void close() {

        }

        @Override
        public void configure(Map<String, ?> configs) {

        }
    }
}
