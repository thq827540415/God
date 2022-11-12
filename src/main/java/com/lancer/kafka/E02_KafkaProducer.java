package com.lancer.kafka;

import lombok.Cleanup;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class E02_KafkaProducer {
    public static void main(String[] args) throws IOException, InterruptedException {
        Properties p = new Properties();
        p.load(E02_KafkaProducer.class.getClassLoader().getResourceAsStream("producer.properties"));

        @Cleanup KafkaProducer<Integer, String> producer = new KafkaProducer<>(p);

        producer.initTransactions();

        int start = 10;
        int end = start + 10;
        producer.beginTransaction();
        for (int i = start; i < end; i++) {
            // 消息发送失败会自动重试
            producer.send(
                    new ProducerRecord<>("test", i, "11111"),
                    (recordMetadata, e) -> {
                        if (Objects.isNull(e)) {
                            System.out.println("success -> " + recordMetadata.offset());
                        } else {
                            producer.abortTransaction();
                            e.printStackTrace();
                        }
                    });

            TimeUnit.SECONDS.sleep(2);
        }
        producer.commitTransaction();
    }
}
