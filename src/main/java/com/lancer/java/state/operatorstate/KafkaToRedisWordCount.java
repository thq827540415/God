package com.lancer.java.state.operatorstate;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.redis.RedisSink;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper;

import java.util.Arrays;
import java.util.Properties;

/**
 * 保证数据一致性
 * 1.开启checkpoint（偏移量，中间累加的结果保存到StateBackend中）
 * 2.kafka记录偏移量（OperatorState）
 * 3.keyBy后要将聚合的结果保存到KeyedState中（ValueState）
 * 4.最终的结果写入redis中（覆盖）
 */
public class KafkaToRedisWordCount {
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.enableCheckpointing(30000);
        // env.setStateBackend(new FsStateBackend("hdfs://bigdata01:9000/flink_chk"));

        env.setParallelism(5);

        Properties p = new Properties();
        p.load(KafkaToRedisWordCount.class.getClassLoader().getResourceAsStream("consumer.properties"));

        /* 老版本 */
        /*FlinkKafkaConsumer<String> kafkaConsumer = new FlinkKafkaConsumer<>("stateTest", new SimpleStringSchema(), p);
        kafkaConsumer.setCommitOffsetsOnCheckpoints(false); // 等价于commit.offsets.on.checkpoint=false, 不将偏移量写出kafka特殊的topic中
        DataStreamSource<String> source = env.addSource(kafkaConsumer);*/

        /* 新版本 */
        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
                .setTopics("stateTest")
                .setProperties(p)
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStreamSource<String> source = env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "KafkaSource");

        SingleOutputStreamOperator<Tuple2<String, Integer>> resDataStream = source
                .filter(value -> !"".equals(value.trim()))
                .flatMap((value, out) -> Arrays.stream(value.split(",")).forEach(out::collect), TypeInformation.of(String.class))
                .map(value -> Tuple2.of(value, 1), TypeInformation.of(new TypeHint<Tuple2<String, Integer>>() {
                }))
                .keyBy(t -> t.f0)
                .sum(1);

        resDataStream.addSink(new RedisSink<>(new FlinkJedisPoolConfig.Builder().setHost("bigdata01").setDatabase(0).build(), new RedisWordCountMapper()));

        env.execute();
    }

    private static class RedisWordCountMapper implements RedisMapper<Tuple2<String, Integer>> {

        /**
         * 以何种方式写入到redis
         * @return
         */
        @Override
        public RedisCommandDescription getCommandDescription() {
            return new RedisCommandDescription(RedisCommand.HSET, "WordCount");
        }

        @Override
        public String getKeyFromData(Tuple2<String, Integer> data) {
            return data.f0;
        }

        @Override
        public String getValueFromData(Tuple2<String, Integer> data) {
            return data.f1 + "";
        }
    }
}
