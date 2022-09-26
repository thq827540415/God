package com.lancer.java.savepoint;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.redis.RedisSink;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper;

import java.util.Arrays;
import java.util.Properties;

/**
 * 在命令行中提交job时，可以指定一个savepoint或checkpoint的目录，使用-s指定目录
 * 停止时，建议使用flink stop -p <savepointDir> jobID，去停止job，并指定savepoint的目录，老版的flink cancel命令将会被Deprecated
 * checkpoint和savepoint的区别：
 *      checkpoint是flink job定期将状态保存起来的对应的一个目录
 *      savepoint是stop任务时，人为指定的一个保存状态数据的目录，实质上HDFS中是将checkpoint中保存下来的那个chkxx的目录移动到指定的位置
 */

public class SavepointDemo {
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.enableCheckpointing(30000);
        env.setStateBackend(new FsStateBackend("hdfs://bigdata01:9000/flink_chk"));

        // 默认情况下，job被cancel后，其checkpoint目录中保存状态数据的目录将会被删除
        // env.getCheckpointConfig().enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.DELETE_ON_CANCELLATION); // 默认模式
        env.getCheckpointConfig().enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION); // 取消job时，目录将会被保存，用于恢复状态

        env.setParallelism(5);

        Properties p = new Properties();
        p.load(SavepointDemo.class.getClassLoader().getResourceAsStream("consumer.properties"));

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
