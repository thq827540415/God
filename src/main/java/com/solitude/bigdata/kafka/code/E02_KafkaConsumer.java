package com.solitude.bigdata.kafka.code;

import lombok.Cleanup;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class E02_KafkaConsumer {
    public static void main(String[] args) throws IOException {
        Properties p = new Properties();
        p.load(E02_KafkaConsumer.class.getClassLoader().getResourceAsStream("consumer.properties"));
        // 对应单个消费者
        @Cleanup KafkaConsumer<Integer, String> consumer = new KafkaConsumer<>(p);

        consumer.subscribe(Collections.singletonList("test"));

        while (true) {
            ConsumerRecords<Integer, String> records = consumer.poll(Duration.ofSeconds(1));
            records.forEach(
                    record -> {
                        System.out.printf("topic: %s\tpartition: %d\toffset: %d\tkey: %d\tvalue: %s\n",
                                record.topic(), record.partition(), record.offset(), record.key(), record.value());
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
            /**
             * 同步提交offset
             *             consumer.commitSync();
             * 异步提交offset
             *             consumer.commitAsync((Map<TopicPartition, OffsetAndMetadata> offset, Exception e) -> {
             *                 if (e != null) {
             *                     System.out.println("commit failed for " + offset);
             *                 }
             *             } );
             */
        }
    }
}
