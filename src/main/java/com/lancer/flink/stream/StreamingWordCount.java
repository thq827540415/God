package com.lancer.flink.stream;

import com.lancer.FlinkEnvUtils;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.ExecutionMode;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.apache.commons.lang3.StringUtils;


public class StreamingWordCount {
    public static void main(String[] args) throws Exception {
        // StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        Configuration config = new Configuration();
        config.setInteger("rest.port", 8888);
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);

        ExecutionConfig executionConfig = env.getConfig();
        // 默认使用PIPELINED处理模式
        executionConfig.setExecutionMode(ExecutionMode.PIPELINED);

        // 没有执行全局聚合算子前，之前的算子组成一个transformation
        // env.disableOperatorChaining();  // 取消chain

        // Source
        DataStreamSource<String> source = env.socketTextStream("bigdata03", 9999);

        // Transformation
        source
                .flatMap(new FlatMapFunction<String, String>() {
                    @Override
                    public void flatMap(String s, Collector<String> collector) throws Exception {
                        String[] words = s.split("\\s+");
                        for (String word : words) {
                            collector.collect(word.toLowerCase().trim());
                        }
                    }
                })
                .filter(new FilterFunction<String>() {
                    @Override
                    public boolean filter(String s) throws Exception {
                        return StringUtils.isNotEmpty(s);
                    }
                }).startNewChain() // 将该算子前面的chain断开
                .map(new MapFunction<String, Tuple2<String, Integer>>() {
                    @Override
                    public Tuple2<String, Integer> map(String s) throws Exception {
                        return new Tuple2<>(s, 1);
                    }
                })
                .keyBy(new KeySelector<Tuple2<String, Integer>, String>() {
                    @Override
                    public String getKey(Tuple2<String, Integer> stringIntegerTuple2) throws Exception {
                        return stringIntegerTuple2.f0;
                    }
                })
                .sum(1).disableChaining() // 将该算子后面的chain断开
                // sink
                .print();

        env.execute("WordCount");
    }
}
