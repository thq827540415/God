package com.lancer.flink.java.window;

import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.TimestampAssigner;
import org.apache.flink.api.common.eventtime.TimestampAssignerSupplier;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.time.Duration;

public class WindowApplyDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        env.setParallelism(2);

        // 默认200ms
        // env.getConfig().setAutoWatermarkInterval(200);

        DataStreamSource<String> source = env.socketTextStream("localhost", 9999);

        // 1000,spark,2
        SingleOutputStreamOperator<String> lineWithWaterMark =
                source
                        .assignTimestampsAndWatermarks(
                                WatermarkStrategy.
                                        <String>forBoundedOutOfOrderness(Duration.ofSeconds(0))
                                        .withTimestampAssigner(new SerializableTimestampAssigner<String>() {
                                            @Override
                                            public long extractTimestamp(String element, long recordTimestamp) {
                                                return Long.parseLong(element.split(",")[0].trim());
                                            }
                                        }));

        lineWithWaterMark
                .map(value ->
                        Tuple2.of(value.split(",")[1].trim(),
                        Integer.parseInt(value.split(",")[2])), Types.TUPLE(Types.STRING, Types.INT))
                .keyBy(value -> value.f0)
                .window(TumblingEventTimeWindows.of(Time.seconds(5)))
                .process(
                        new ProcessWindowFunction<Tuple2<String, Integer>, Tuple2<String, Integer>, String, TimeWindow>() {

                            @Override
                            public void open(Configuration parameters) throws Exception {
                            }

                            @Override
                            public void process(String s,
                                                Context context,
                                                Iterable<Tuple2<String, Integer>> input,
                                                Collector<Tuple2<String, Integer>> out) throws Exception {
                                System.out.println("key：" + s + "，window：" + context.window());

                                int total = 0;
                                for (Tuple2<String, Integer> tp : input) {
                                    total += tp.f1;
                                }
                                // 输出
                                out.collect(Tuple2.of(s + " == " + context.currentWatermark(), total));
                            }
                        })
                /**
                 * 当窗口触发后，每一个组（key相同）都会调用一次, 全量计算
                 * @param s 分组的key
                 * @param window 当前window对象
                 * @param input 当前窗口攒的数据（将key相同的数据放入到一个集合中）
                 * @param out 输出的数据
                 */
                /*
                .apply(new WindowFunction<Tuple2<String, Integer>, Tuple2<String, Integer>, String, TimeWindow>() {


                    @Override
                    public void apply(String s, TimeWindow window, Iterable<Tuple2<String, Integer>> input, Collector<Tuple2<String, Integer>> out) throws Exception {

                        System.out.println("key：" + s + "，window：" + window);

                        int total = 0;
                        for (Tuple2<String, Integer> tp : input) {
                            total += tp.f1;
                        }
                        // 输出
                        out.collect(Tuple2.of(s, total));
                    }
                })*/
                .print();

        env.execute();
    }
}
