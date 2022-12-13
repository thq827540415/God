package com.ava.bigdata.kafka.code.producer;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Map;
import java.util.Objects;

public class E04_Interceptor {
    /**
     * 每条消息加上前缀
     */
    public static class MyInterceptor implements ProducerInterceptor<Integer, String> {

        private long sendSuccess = 0;
        private long sendFailure = 0;

        /**
         * 最好不要修改ProducerRecord的topic、key和partition等信息。
         * 修改key不仅会影响分区的计算，同样会影响broker端日志压缩(Log Compaction)的功能
         */
        @Override
        public ProducerRecord<Integer, String> onSend(ProducerRecord<Integer, String> record) {
            String modifiedValue = "prefix1-" + record.value();
            return new ProducerRecord<>(
                    record.topic(),
                    record.partition(),
                    record.timestamp(),
                    record.key(),
                    modifiedValue,
                    record.headers());
        }

        /**
         * 在消息被ack之前或消息发送失败时调用生产者拦截器的onAcknowledgement()，优先于用户设定的Callback之前执行，
         * 该方法运行在Producer的I/O线程中，所以这个方法中实现的代码逻辑越简单越好，否则会影响消息的发送速度
         */
        @Override
        public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
            if (Objects.isNull(exception)) {
                sendSuccess++;
            } else {
                sendFailure++;
            }
        }

        @Override
        public void close() {
            double successRatio = (double) sendSuccess / (sendFailure + sendSuccess);
            System.out.println("发送成功率 = " +  String.format("%f", successRatio * 100) + "%");
        }

        @Override
        public void configure(Map<String, ?> configs) {

        }
    }
}
