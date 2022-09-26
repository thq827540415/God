package com.lancer.java.state.keyedstate;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

import java.util.HashMap;
import java.util.Map;

public class MyStateDemo01 {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        env.enableCheckpointing(30000);

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

                // 自定义实现求和方法，实际上使用Map
                .map(new RichMapFunction<Tuple2<String, Integer>, Tuple2<String, Integer>>() {

                    private final Map<String, Integer> counter = new HashMap<>();

                    @Override
                    public Tuple2<String, Integer> map(Tuple2<String, Integer> value) throws Exception {
                        // 获取当前输入的单词
                        String word = value.f0;
                        // 获取当前输入的次数
                        Integer current = value.f1;
                        Integer history = counter.getOrDefault(word, 0);
                        Integer total = history + current;
                        counter.put(word, total);
                        value.f1 = total;
                        return value;
                    }
                })
                .print();
        env.execute();
    }
}
