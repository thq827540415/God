package com.lancer.stream.sink;

import com.lancer.FlinkEnvUtils;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;

import javax.annotation.Nullable;
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

        DataStreamSource<String> source = env.socketTextStream("localhost", 9999);

        source.addSink(getKafkaSink(p));

        env.execute(E02_KafkaSink.class.getSimpleName());
    }


    private static FlinkKafkaProducer<String> getKafkaSink(Properties p) {

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
}
