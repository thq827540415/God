package com.lancer.flink.java.process;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;


/**
 * 使用ProcessFunction注册ProcessingTime的定时器
 * <p>
 * 只能在KeyedStream上注册定时器
 */
public class ProcessingTimeProcessFunctionDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        env.enableCheckpointing(30000);

        env.setParallelism(2);

        // word,3
        env
                .socketTextStream("bigdata01", 9999)
                .map(new MapFunction<String, Tuple2<String, Integer>>() {
                    @Override
                    public Tuple2<String, Integer> map(String value) throws Exception {
                        String[] fields = value.split(",");
                        return Tuple2.of(fields[0], Integer.parseInt(fields[1]));
                    }
                })
                .keyBy(value -> value.f0)
                // 实现类似滚动窗口功能，实现增量聚合
                .process(new KeyedProcessFunction<String, Tuple2<String, Integer>, Tuple2<String, Integer>>() {

                    private transient ValueState<Integer> valueState;

                    @Override
                    public void open(Configuration parameters) {
                        ValueStateDescriptor<Integer> valueStateDescriptor = new ValueStateDescriptor<>("count-test", Integer.class);
                        valueState = getRuntimeContext().getState(valueStateDescriptor);
                    }

                    // 当processElement方法中注册的定时器触发后，会调用该方法
                    @Override
                    public void onTimer(long timestamp, OnTimerContext ctx, Collector<Tuple2<String, Integer>> out) throws Exception {
                        // System.out.println("onTimer执行，key为：" + ctx.getCurrentKey() + "，subtask的下标为：" + getRuntimeContext().getIndexOfThisSubtask());
                        out.collect(Tuple2.of(ctx.getCurrentKey() + "触发的时间为：" + timestamp, valueState.value()));
                        valueState.clear();
                    }

                    @Override
                    public void processElement(Tuple2<String, Integer> value, Context ctx, Collector<Tuple2<String, Integer>> out) throws Exception {
                        // 注册processingTime定时器，每个key值都会有一个定时器
                        // ctx.timerService().registerProcessingTimeTimer(System.currentTimeMillis() + 30000); // 当前时间的30s之后触发
                        // System.out.println("processElement方法被调用，值为：" + value + ", 进入该方法的key为：" + ctx.getCurrentKey() + "，进入的subtask下标为：" + getRuntimeContext().getIndexOfThisSubtask());
                        // 如果注册了多个相同的定时器，只会触发一次后面注册的定时器，将前面相同时间的定时器覆盖掉
                        long currentTimeMillis = ctx.timerService().currentProcessingTime();
                        // 16:30:08 -> 16:30:00
                        // 每一分钟触发一次，类似于1分钟滚动一次窗口
                        // 16:30:08 - 00:00:08 + (00:00:60=00:01:00) => 16:30:00 + 00:01:00 => 16:31:00
                        long trigger = currentTimeMillis - currentTimeMillis % 60000 + 60 * 1000;
                        // 每条数据都会注册一个定时器
                        ctx.timerService().registerProcessingTimeTimer(trigger);

                        // 增量聚合
                        Integer history = valueState.value();
                        if (history != null) {
                            value.f1 += history;
                        }
                        valueState.update(value.f1);
                    }
                })
                .print();

        env.execute();
    }
}
