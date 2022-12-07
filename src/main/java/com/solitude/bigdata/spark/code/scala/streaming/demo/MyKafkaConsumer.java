package com.solitude.bigdata.spark.code.scala.streaming.demo;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.Collections;
import java.util.Properties;

/**
 * @Author lancer
 * @Date 2022/2/18 7:06 下午
 * @Description
 */
public class MyKafkaConsumer {
    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        properties.load(MyKafkaConsumer.class.getClassLoader().getResourceAsStream("consumer.properties"));

        KafkaConsumer<Integer, String> consumer = new KafkaConsumer<>(properties);

        // 订阅topic
        consumer.subscribe(Collections.singletonList("project"));

        // 从哪Kafka对应的topic中拉取数据
        while (true) {
            ConsumerRecords<Integer, String> consumerRecords = consumer.poll(1000);
            for (ConsumerRecord<Integer, String> record : consumerRecords) {
                System.out.printf("topic:%s\tpartition:%d\toffset:%d\tkey:%d\tvalue:%s%n",
                        record.topic(), record.partition(), record.offset(), record.key(), record.value());
                Thread.sleep(1000);
            }

            /**
             * 当offset提交方式为手动提交时，使用
             * 1. 同步提交offset --> 当前线程会阻塞，直到offset提交成功；会产生漏消费；
             *      consumer.commitSync();
             * 2. 异步提交offset --> 没有失败重试机制，故有可能提交失败；会产生重复消费
             *      consumer.commitAsync((Map<TopicPartition, OffsetAndMetadata> offsets, Exception e) -> {
             *                 if (e != null) {
             *                     System.err.println("Commit failed for" + offsets);
             *                 }
             *             });
             */
        }
    }
}
