package com.lancer.bigdata.flink.stream.source;

import com.lancer.bigdata.util.FlinkEnvUtils;
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
        /*DataStreamSource<String> kafkaSource = env.fromSource(
                newGetKafkaSource(p),
                WatermarkStrategy.noWatermarks(),
                "Kafka Source");*/

        kafkaSource.print();

        env.execute(E02_KafkaSource.class.getSimpleName());
    }

    /**
     * 采用Kafka SourceFunction的方式
     *      目前这种方式无法保证Exactly Once，Flink的Source消费完数据后，将偏移量定期写入到kafka的__consumer_offsets中，
     *      这种方式虽然可以记录偏移量，但是无法保证Exactly Once
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

        // 分配时间戳和水位线
        kafkaSource.assignTimestampsAndWatermarks(WatermarkStrategy.noWatermarks());

        return kafkaSource;
    }

    /**
     * 采用Kafka Source的方式
     *      Flink会把kafka消费者的消费位移记录在算子状态中，这样就实现了消费offset状态的容错，从而可以支持端到端的Exactly Once
     */
    private static KafkaSource<String> newGetKafkaSource(Properties p) {
        // 定义消费的分区
        HashSet<TopicPartition> topicPartitions = new HashSet<>(Arrays.asList(
                new TopicPartition("test", 0),
                new TopicPartition("test", 1),
                new TopicPartition("test", 2)
        ));

        Map<TopicPartition, Long> specificStartOffsets = new HashMap<>();
        specificStartOffsets.put(new TopicPartition("test", 0), 1L);
        specificStartOffsets.put(new TopicPartition("test", 1), 2L);
        specificStartOffsets.put(new TopicPartition("test", 2), 3L);

        return KafkaSource.<String>builder()
                .setBootstrapServers("kafka01:9092,kafka02:9093,kafka03:9094")
                .setTopics("test")
                .setGroupId("test-group")
                .setProperties(p)
                .setPartitions(topicPartitions)
                // .setStartingOffsets(OffsetsInitializer.offsets(specificStartOffsets))
                .setStartingOffsets(OffsetsInitializer.latest())
                // .setDeserializer(KafkaRecordDeserializationSchema.valueOnly(StringDeserializer.class))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();
    }

}
