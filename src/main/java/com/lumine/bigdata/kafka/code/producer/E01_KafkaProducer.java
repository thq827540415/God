package com.lumine.bigdata.kafka.code.producer;

import lombok.Cleanup;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class E01_KafkaProducer {
    public static void main(String[] args) throws IOException, InterruptedException {
        Properties p = new Properties();
        p.load(E01_KafkaProducer.class.getClassLoader().getResourceAsStream("producer.properties"));

        // KafkaProducer是线程安全的
        @Cleanup KafkaProducer<Integer, String> producer = new KafkaProducer<>(p);

        producer.initTransactions();

        int start = 10;
        int end = start + 10;
        producer.beginTransaction();
        for (int i = start; i < end; i++) {
            try {
                // 消息发送失败会自动重试
                // 发送消息主要有三种模式：fire-and-forget、sync（调用get方法）、async（加上回调）
                // 消息有可能需要经过拦截器Interceptor、序列化器Serializer和分区器Partitioner才能真正发往broker
                producer.send(
                        // 当ProducerRecord中没有指定partition字段，那么就要依赖分区器
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
            } catch (Exception e) {
                producer.abortTransaction();
                e.printStackTrace();
            }

        }
        producer.commitTransaction();
    }
}
