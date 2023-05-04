package com.ivi.code.stream;

import com.ivi.consts.KafkaConstants;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.KafkaSourceBuilder;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkFixedPartitioner;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

import java.io.IOException;
import java.util.Properties;

public class FlinkKafka {
    public static void main(String[] args) {
    }

    /**
     * Flink会把kafka消费者的消费位移记录在算子状态中，这样就实现了消费offset状态的容错
     */
    public static <T> KafkaSource<T> getKafkaSource(Properties props,
                                                    String groupId,
                                                    KafkaRecordDeserializationSchema<T> deserializationSchema,
                                                    String... topics) {
        
        KafkaSourceBuilder<T> builder = KafkaSource.<T>builder()
                .setBootstrapServers(KafkaConstants.BOOTSTRAP_SERVERS_OR_BROKER_LIST)
                .setGroupId(groupId)
                .setProperties(props)
                .setTopics(topics);


        ImmutableSet<TopicPartition> topicPartitions = ImmutableSet.of(
                new TopicPartition("test", 0),
                new TopicPartition("test", 1),
                new TopicPartition("test", 2));
        // 定义消费的分区
        builder.setPartitions(topicPartitions);


        ImmutableMap<TopicPartition, Long> specificStartOffsets = ImmutableMap.of(
                new TopicPartition("test", 0), 0L,
                new TopicPartition("test", 1), 0L,
                new TopicPartition("test", 2), 0L);
        // 从指定分区的偏移量处开始消费
        builder.setStartingOffsets(OffsetsInitializer.offsets(specificStartOffsets));

        if (deserializationSchema.getProducedType() == null) {
            builder.setDeserializer(new KafkaRecordDeserializationSchema<T>() {
                @Override
                public void deserialize(ConsumerRecord<byte[], byte[]> consumerRecord,
                                        Collector<T> collector) throws IOException {

                }

                @Override
                public TypeInformation<T> getProducedType() {
                    return null;
                }
            });
        }
        return builder.build();
    }

    /**
     * 底层利用了Kafka producer的事务机制，结合Flink的checkpoint实现EOS语义
     */
    public static <T> KafkaSink<T> getKafkaSink(Properties props, String topic) {
        return KafkaSink.<T>builder()
                .setBootstrapServers(KafkaConstants.BOOTSTRAP_SERVERS_OR_BROKER_LIST)
                .setDeliverGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                // 如果使用EXACTLY_ONCE，需要使用事务ID
                .setTransactionalIdPrefix("test-")
                .setKafkaProducerConfig(props)
                .setRecordSerializer(
                        KafkaRecordSerializationSchema.<T>builder()
                                .setTopic(topic)
                                .setKeySerializationSchema((SerializationSchema<T>) element -> new byte[0])
                                .setKeySerializationSchema((SerializationSchema<T>) element -> new byte[0])
                                .setPartitioner(new FlinkFixedPartitioner<>())
                                .build())
                .build();
    }
}
