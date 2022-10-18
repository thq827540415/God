package com.lancer.flink.stream.source;

import com.lancer.FlinkEnvUtils;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.internals.KafkaTopicPartition;
import org.apache.kafka.common.TopicPartition;

import java.util.*;


/**
 * @Author lancer
 * @Date 2022/6/9 00:25
 * @Description 消费kafka数据
 */
public class E02_KafkaSource {
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        Properties p = new Properties();
        p.load(E02_KafkaSource.class.getClassLoader().getResourceAsStream("consumer.properties"));

        DataStreamSource<String> kafkaSource = env.addSource(oldGetKafkaSource(p));
        // DataStreamSource<String> kafkaSource = env.fromSource(newGetKafkaSource(p), WatermarkStrategy.noWatermarks(), "Kafka Source");

        kafkaSource.print();

        env.execute(E02_KafkaSource.class.getSimpleName());
    }

    /**
     * 采用Kafka SourceFunction的方式
     */
    private static FlinkKafkaConsumer<String> oldGetKafkaSource(Properties p) {

        Map<KafkaTopicPartition, Long> specificStartOffsets = new HashMap<>();
        specificStartOffsets.put(new KafkaTopicPartition("test", 0), 1L);
        specificStartOffsets.put(new KafkaTopicPartition("test", 1), 2L);
        specificStartOffsets.put(new KafkaTopicPartition("test", 2), 3L);

        FlinkKafkaConsumer<String> kafkaSource = (FlinkKafkaConsumer<String>) new FlinkKafkaConsumer<>(
                "test",
                new SimpleStringSchema(),
                p)
                // 从指定分区的偏移量处开始消费
                .setStartFromSpecificOffsets(specificStartOffsets);

        kafkaSource.assignTimestampsAndWatermarks(WatermarkStrategy.noWatermarks());

        return kafkaSource;
    }

    /**
     * 采用Kafka Source的方式
     */
    private static KafkaSource<String> newGetKafkaSource(Properties p) {
        // 定义消费的分区
        HashSet<TopicPartition> topicPartitions = new HashSet<>(Arrays.asList(
                new TopicPartition("test", 0),
                new TopicPartition("test", 1),
                new TopicPartition("test", 2)
        ));

        return KafkaSource.<String>builder()
                .setTopics("test")
                .setGroupId("test-group")
                .setProperties(p)
                .setPartitions(topicPartitions)
                .setBootstrapServers("kafka01:9092,kafka02:9093,kafka03:9094")
                .setStartingOffsets(OffsetsInitializer.latest())
                // .setDeserializer(KafkaRecordDeserializationSchema.valueOnly(StringDeserializer.class))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();
    }

}
