package com.shadow.garden.bigdata.spark.code.scala.streaming.demo;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Properties;

/**
 * @Author lancer
 * @Date 2022/2/18 7:52 下午
 * @Description
 */
public class MyKafkaProducer {
    public static void main(String[] args) throws Exception {
        Properties properties = new Properties();
        properties.load(MyKafkaProducer.class.getClassLoader().getResourceAsStream("producer.properties"));

        KafkaProducer<Integer, String> producer = new KafkaProducer<>(properties);

        int start = 10;
        int end = start + 10;
        for (int i = start; i < end; i++) {
            // send()方法后调用get()方法，将变成同步发送
            // send()方法中第二个参数，表示回调函数，在producer接收到ack时调用，为异步调用
            // 消息发送失败，会自动重试
            producer.send(new ProducerRecord<>("test", i, "11111"), (recordMetadata, e) -> {
                if (e == null) {
                    System.out.println("success->" + recordMetadata.offset());
                } else {
                    e.printStackTrace();
                }
            });
            Thread.sleep(2000);

        }
        producer.close();
    }
}
