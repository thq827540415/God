package com.lancer.flink.java.window.timewindow.eventtime;

import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

public class SlidingEventTimeWindowKeyedDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        test01(env);

        env.execute();
    }

    private static void test01(StreamExecutionEnvironment env) {

        DataStreamSource<String> source = env.socketTextStream("bigdata01", 9999);

        SingleOutputStreamOperator<String> lineWithWartMark = source.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<String>(Time.seconds(0)) {
            @Override
            public long extractTimestamp(String element) {
                return Long.parseLong(element.split(",")[0].trim());
            }
        });

        WindowedStream<Tuple2<String, Integer>, String, TimeWindow> slidingEventTimeWindowDataStream = lineWithWartMark
                .map(value -> Tuple2.of(value.split(",")[1].trim(), 1), TypeInformation.of(new TypeHint<Tuple2<String, Integer>>() {
                }))
                .keyBy(value -> value.f0)
                .window(SlidingEventTimeWindows.of(Time.seconds(10), Time.seconds(8)));// 传入windowAll方法中的参数为WindowAssigner（窗口分配器）

        SingleOutputStreamOperator<Tuple2<String, Integer>> sumDataStream = slidingEventTimeWindowDataStream.sum(1);

        sumDataStream.print();
    }
}
