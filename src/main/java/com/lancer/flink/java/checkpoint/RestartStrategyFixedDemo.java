package com.lancer.flink.java.checkpoint;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * 重启策略 之 固定次数
 */
public class RestartStrategyFixedDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        // 错误次数超过3次，则退出程序
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, 2000));
        // env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, Time.seconds(2)));

        env
                .socketTextStream("bigdata01", 9999)
                .filter(value -> !"".equals(value))
                .map(new MapFunction<String, Tuple2<String, Integer>>() {
                    @Override
                    public Tuple2<String, Integer> map(String value) throws Exception {
                        if ("error".equals(value)) {
                            throw new RuntimeException("test error");
                        }
                        return Tuple2.of(value, 1);
                    }
                }, TypeInformation.of(new TypeHint<Tuple2<String, Integer>>() {
                }))
                .keyBy(new KeySelector<Tuple2<String, Integer>, String>() {
                    @Override
                    public String getKey(Tuple2<String, Integer> value) throws Exception {
                        return value.f0;
                    }
                })
                .sum(1)
                .print();

        env.execute();
    }
}
