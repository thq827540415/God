package com.shadow.garden.bigdata.flink.code.cdc;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.shadow.garden.consts.Consts;
import com.shadow.garden.bigdata.flink.code.stream.sink.E02_KafkaSink;
import com.shadow.garden.util.FlinkEnvUtils;
import com.shadow.garden.util.JsonUtils;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.DebeziumDeserializationSchema;
import com.ververica.cdc.debezium.DebeziumSourceFunction;
import io.debezium.data.Envelope;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

/**
 * @Author lancer
 * @Date 2022/4/18 20:08
 * @Description
 */
public class MySqlSource {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        // env.enableCheckpointing(3000);

        DebeziumSourceFunction<String> source = com.ververica.cdc.connectors.mysql.MySqlSource.<String>builder()
                .hostname(Consts.MYSQL_HOST)
                .port(Consts.MYSQL_PORT)
                .databaseList("project01")
                .tableList("project01.cdc_test")
                .username(Consts.MYSQL_USERNAME)
                .password(Consts.MYSQL_PASSWORD)
                .deserializer(new CustomDeserializationSchema())
                .startupOptions(StartupOptions.initial())
                .build();

        Properties p = new Properties();
        p.load(E02_KafkaSink.class.getClassLoader().getResourceAsStream("producer.properties"));

        KafkaSerializationSchema<String> serializationSchema = (element, timestamp) ->
                new ProducerRecord<>(
                        "test",
                        element.getBytes(StandardCharsets.UTF_8));

        FlinkKafkaProducer<String> kafkaSink = new FlinkKafkaProducer<>(
                "test",
                serializationSchema,
                p,
                FlinkKafkaProducer.Semantic.EXACTLY_ONCE
        );

        env
                .addSource(source)
                .addSink(kafkaSink);

        env.execute("Print MYSQL Snapshot + Binlog");
    }

    private static class CustomDeserializationSchema implements DebeziumDeserializationSchema<String> {
        @Override
        public void deserialize(SourceRecord sourceRecord, Collector<String> collector) throws Exception {

            ObjectNode result = JsonUtils.getObjectNode();

            // 获取库名+表名
            String[] tblAndDbName = sourceRecord.topic().split("\\.");
            result
                    .put("db", tblAndDbName[1])
                    .put("tbl", tblAndDbName[2]);

            // 获取before数据
            ObjectNode beforeJson = result.putObject("before");
            Struct before = ((Struct) sourceRecord.value()).getStruct("before");
            if (Objects.nonNull(before)) {
                for (Field field : before.schema().fields()) {
                    beforeJson.put(field.name(), (String) before.get(field));
                }
            }

            // 获取after数据
            ObjectNode afterJson = result.with("after");
            Struct after = ((Struct) sourceRecord.value()).getStruct("after");
            if (Objects.nonNull(after)) {
                for (Field field : after.schema().fields()) {
                    afterJson.put(field.name(), (String) after.get(field));
                }
            }

            // 获取操作类型
            Envelope.Operation operation = Envelope.operationFor(sourceRecord);
            result.put("op", operation.code());

            collector.collect(result.toString());
        }

        @Override
        public TypeInformation<String> getProducedType() {
            return BasicTypeInfo.STRING_TYPE_INFO;
        }
    }
}
