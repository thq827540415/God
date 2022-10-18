package com.lancer.flink.java.checkpoint;

import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class CheckpointingDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        // env.enableCheckpointing(1000, CheckpointingMode.EXACTLY_ONCE);
        // env.enableCheckpointing(1000, CheckpointingMode.AT_LEAST_ONCE);
        // env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);

        // 设置checkpoint的时间间隔->ms
        // env.getCheckpointConfig().setCheckpointInterval(10000);

        // 存储state在stateBackend，默认在JM内存中
        // 开启checkpoint，默认的重启策略就是无限重启 --> 固定次数为Integer.MAX_VALUE
        // 没有开启checkpoint，则默认为noStrategy

        env.enableCheckpointing(Time.seconds(60).toMilliseconds());

        env
                .socketTextStream("bigdata01", 9999)
                .filter(value -> !"".equals(value))
                .map(value -> {
                    if ("error".equals(value)) {
                        throw new RuntimeException("test error");
                    }
                    return Tuple2.of(value, 1);
                }, TypeInformation.of(new TypeHint<Tuple2<String, Integer>>() {
                }))
                .keyBy(value -> value.f0)
                .sum(1)
                .print();

        env.execute();
    }
}
