package com.solitude.bigdata.flink.code.stream.transformation.process;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.time.Duration;


/**
 * 使用ProcessFunction注册EventTime的定时器
 * <p>
 * 只能在KeyedStream上注册定时器
 * 增量聚合
 */
public class IncrementEventTimeProcessFunctionDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        env.enableCheckpointing(30000);

        env.setParallelism(2);

        // 1000,word,3
        env
                .socketTextStream("bigdata01", 9999)
                // 生成watermark
                .assignTimestampsAndWatermarks(WatermarkStrategy
                        .<String>forBoundedOutOfOrderness(Duration.ZERO)
                        .withTimestampAssigner((element, recordTimestamp) -> Long.parseLong(element.split(",")[0].trim())))
                .map( value -> {
                    String[] fields = value.split(",");
                    return Tuple2.of(fields[1], Integer.parseInt(fields[2]));
                }, Types.TUPLE(Types.STRING, Types.INT))
                .keyBy(value -> value.f0)
                // 实现类似滚动窗口功能，实现增量聚合,在EventTime只能够实现一个定时器，一个定时器触发
                .process(new KeyedProcessFunction<String, Tuple2<String, Integer>, Tuple2<String, Integer>>() {

                    private transient ValueState<Integer> valueState;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        ValueStateDescriptor<Integer> valueStateDescriptor = new ValueStateDescriptor<>("count-test", Integer.class);
                        valueState = getRuntimeContext().getState(valueStateDescriptor);
                    }

                    @Override
                    public void processElement(Tuple2<String, Integer> value, Context ctx, Collector<Tuple2<String, Integer>> out) throws Exception {

                        long currentWatermark = ctx.timerService().currentWatermark();
                        // 每10s触发一次 模拟窗口边界为 10000 - 1 = 9999，当watermark到达9999时，触发
                        long trigger = currentWatermark - currentWatermark % 10000 + 10000 - 1;

                        // 因为未输入数据时，上游的watermark会一直发送负的watermark，也定义了一个定时器，所以当你输入第一条数据的时候就会触发一次定时器了
                        if (trigger >= 0) {
                            // 只有watermark >= 注册的时间，才会触发，所以当你输入10000,spark,1时，真正的时间才是10000 - 0 - 1 = 9999,还没有达到注册的时间，所以没有触发
                            ctx.timerService().registerEventTimeTimer(trigger);
                            // System.out.println("注册的时间为：" + trigger); // 第一次的waterMark为负数
                        }

                        // 增量聚合
                        Integer history = valueState.value();
                        if (history != null) {
                            value.f1 += history;
                        }
                        valueState.update(value.f1);
                    }

                    // 当processElement方法中注册的定时器触发后，会调用该方法
                    @Override
                    public void onTimer(long timestamp, OnTimerContext ctx, Collector<Tuple2<String, Integer>> out) throws Exception {
                        // System.out.println("onTimer执行，key为：" + ctx.getCurrentKey() + "，subtask的下标为：" + getRuntimeContext().getIndexOfThisSubtask());
                        out.collect(Tuple2.of(ctx.getCurrentKey(), valueState.value()));
                        valueState.clear();
                    }
                })
                .print();

        env.execute();
    }
}
