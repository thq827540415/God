package com.ivi.code.stream.transformation.process;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


/**
 * 使用ProcessFunction注册EventTime的定时器
 * <p>
 * 只能在KeyedStream上注册定时器
 * 全量聚合
 */
public class CompleteEventTimeProcessFunctionDemo {
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
                .map(new MapFunction<String, Tuple2<String, Integer>>() {
                    @Override
                    public Tuple2<String, Integer> map(String value) throws Exception {
                        String[] fields = value.split(",");
                        return Tuple2.of(fields[1], Integer.parseInt(fields[2]));
                    }
                })
                .keyBy(value -> value.f0)
                // 实现类似滚动窗口功能，实现增量聚合,在EventTime只能够实现一个定时器，一个定时器触发
                .process(new KeyedProcessFunction<String, Tuple2<String, Integer>, Tuple2<String, Integer>>() {

                    private transient ListState<Integer> listState;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        ListStateDescriptor<Integer> listStateDescriptor = new ListStateDescriptor<>("list-test", Integer.class);
                        listState = getRuntimeContext().getListState(listStateDescriptor);
                    }

                    // 当processElement方法中注册的定时器触发后，会调用该方法
                    @Override
                    public void onTimer(long timestamp, OnTimerContext ctx, Collector<Tuple2<String, Integer>> out) throws Exception {

                        List<Integer> list1 = (ArrayList<Integer>) listState.get();
                        list1.sort((a,  b) -> b - a);
                        for (int i = 0; i < Math.min(list1.size(), 3); i++) {
                            out.collect(Tuple2.of(ctx.getCurrentKey(), list1.get(i)));
                        }
                    }

                    @Override
                    public void processElement(Tuple2<String, Integer> value, Context ctx, Collector<Tuple2<String, Integer>> out) throws Exception {

                        long currentWatermark = ctx.timerService().currentWatermark();
                        long trigger = currentWatermark - currentWatermark % 10000 + 10000 - 1; // 每10s触发一次 模拟窗口边界为 10000 - 1 = 9999，当watermark到达9999时，触发

                        // 因为未输入数据时，上游的watermark会一直发送负的watermark，也定义了一个定时器，所以当你输入第一条数据的时候就会触发一次定时器了
                        if (trigger >= 0) {
                            ctx.timerService().registerEventTimeTimer(trigger);  // 只有watermark >= 注册的时间，才会触发，所以当你输入10000,spark,1时，真正的时间才是10000 - 0 - 1 = 9999,还没有达到注册的时间，所以没有触发
                            // System.out.println("注册的时间为：" + trigger); // 第一次的waterMark为负数
                        }
                        // 先放入ListState中
                        listState.add(value.f1);
                    }
                })
                .print();

        env.execute();
    }
}
