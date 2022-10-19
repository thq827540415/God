package com.lancer.flink.stream.sink;

import com.lancer.FlinkEnvUtils;
import com.lancer.consts.UsualConsts;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkFixedPartitioner;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * @Author lancer
 * @Date 2022/6/15 19:05
 * @Description
 */
public class E02_KafkaSink {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();
        Properties p = new Properties();
        p.load(E02_KafkaSink.class.getClassLoader().getResourceAsStream("producer.properties"));

        DataStreamSource<String> source = env.socketTextStream(UsualConsts.NC_HOST, 9999);

        source.addSink(oldGetKafkaSink(p));
        // source.sinkTo(newGetKafkaSink(p));

        env.execute(E02_KafkaSink.class.getSimpleName());
    }


    /**
     * 采用Kafka SinkFunction的方式
     */
    private static FlinkKafkaProducer<String> oldGetKafkaSink(Properties p) {

        KafkaSerializationSchema<String> serializationSchema = (element, timestamp) ->
                new ProducerRecord<>(
                        "test",
                        element.getBytes(StandardCharsets.UTF_8));

        return new FlinkKafkaProducer<>(
                "test",
                serializationSchema,
                p,
                FlinkKafkaProducer.Semantic.EXACTLY_ONCE
        );
    }

    /**
     * 采用Kafka Sink的方式
     *      KafkaSink是能结合Flink的Checkpoint机制，来支持Exactly Once语义的
     *      底层是利用了Kafka producer的事务机制
     */
    private static KafkaSink<String> newGetKafkaSink(Properties p) {

        return KafkaSink.<String>builder()
                .setBootstrapServers("kafka01:9092,kafka02:9093,kafka03:9094")
                .setDeliverGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                // 如果使用了EXACTLY_ONCE，则也需要使用setTransactionalIdPrefix
                .setTransactionalIdPrefix("test-")
                .setKafkaProducerConfig(p)
                .setRecordSerializer(
                        KafkaRecordSerializationSchema.<String>builder()
                                .setTopic("test")
                                // .setKafkaValueSerializer(StringSerializer.class)
                                .setKeySerializationSchema(new SimpleStringSchema())
                                .setValueSerializationSchema(new SimpleStringSchema())
                                .setPartitioner(new FlinkFixedPartitioner<>())
                                .build()
                )
                .build();
    }
}
