package com.lancer.flink.java.process;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

/**
 * keyBy后对KeyedStream调用ProcessFunction，可以使用状态
 *
 * keyBy后使用ValueState实现sum的功能而且可以容错
 */
public class MyKeyedStreamProcessFunctionDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        env.enableCheckpointing(10000);

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
                .process(new KeyedProcessFunction<String, Tuple2<String, Integer>, Tuple2<String, Integer>>() {

                    // 只能在KeyedProcessFunction中使用
                    private transient ValueState<Integer> valueState;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        ValueStateDescriptor<Integer> valueStateDescriptor = new ValueStateDescriptor<>("count-state", Integer.class);
                        valueState = getRuntimeContext().getState(valueStateDescriptor);
                    }

                    @Override
                    public void processElement(Tuple2<String, Integer> value, Context ctx, Collector<Tuple2<String, Integer>> out) throws Exception {
                        // ctx.getCurrentKey();
                        Integer history = valueState.value();
                        if (history != null) {
                            value.f1 += history;
                        }
                        valueState.update(value.f1);
                        out.collect(value);
                    }
                })
                // 在ProcessFunction中使用状态已经被废弃
                /*.process(new ProcessFunction<Tuple2<String, Integer>, Tuple2<String, Integer>>() {


                    private transient ValueState<Integer> valueState;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        ValueStateDescriptor<Integer> valueStateDescriptor = new ValueStateDescriptor<>("count-state", Integer.class);
                        valueState = getRuntimeContext().getState(valueStateDescriptor);
                    }

                    @Override
                    public void processElement(Tuple2<String, Integer> value, Context ctx, Collector<Tuple2<String, Integer>> out) throws Exception {
                        Integer history = valueState.value();
                        if (history != null) {
                            value.f1 += history;
                        }
                        valueState.update(value.f1);
                        out.collect(value);
                    }
                })*/
                .print();

        env.execute();
    }
}
