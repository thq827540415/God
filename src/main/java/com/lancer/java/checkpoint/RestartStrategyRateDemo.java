package com.lancer.java.checkpoint;

import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * 重启策略 之 固定次数
 */
public class RestartStrategyRateDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        // 指一定时间内，超过指定次数3，程序退出
        // 一定的时间范围，超过了30s这个时间，没达到指定次数，那么重新计数
        // 重启延迟时间
        env.setRestartStrategy(RestartStrategies.failureRateRestart(3, Time.seconds(30), Time.seconds(5)));

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
