package com.lancer.java.state.keyedstate;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

public class ValueStateDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        env.enableCheckpointing(10000);
        // 设置重启策略
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, 2000));
        env.setParallelism(2);

        env
                .socketTextStream("bigdata01", 9999)
                .filter(value -> !"".equals(value))
                .flatMap(new FlatMapFunction<String, String>() {
                    @Override
                    public void flatMap(String value, Collector<String> out) throws Exception {
                        String[] words = value.split(" ");
                        for (String word : words) {
                            out.collect(word);
                        }
                    }
                })
                .map(value -> {
                    if ("error".equals(value)) {
                        throw new RuntimeException("test error");
                    }
                    return Tuple2.of(value, 1);
                }, TypeInformation.of(new TypeHint<Tuple2<String, Integer>>() {
                }))
                .keyBy(value -> value.f0)

                /* 官方做法 */
                // StreamGroupedReduceOperator类中类似
                .map(new RichMapFunction<Tuple2<String, Integer>, Tuple2<String, Integer>>() {

                    // 定义一个保存数值类型的状态
                    private ValueState<Integer> countState;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        // 定义一个状态描述器，通过name找到相应的状态
                        ValueStateDescriptor<Integer> stateDescriptor = new ValueStateDescriptor<>("wc-test", Integer.class);
                        // 初始化或者是恢复状态，底层调用的是keyedStateBackend.getPartitionedState(...)
                        countState = getRuntimeContext().getState(stateDescriptor);
                    }

                    @Override
                    public Tuple2<String, Integer> map(Tuple2<String, Integer> value) throws Exception {
                        Integer history = countState.value();
                        if (history != null) {
                            value.f1 += history;
                        }
                        countState.update(value.f1);
                        return value;
                    }
                })
                .print();

        env.execute();
    }
}
