package com.lancer.flink.java.window.timewindow.eventtime;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

public class SessionEventTimeWindowKeyedDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        test01(env);

        env.execute();
    }

    private static void test01(StreamExecutionEnvironment env) {

        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        DataStreamSource<String> source = env.socketTextStream("bigdata01", 9999);

        SingleOutputStreamOperator<String> lineWithWaterMark = source.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<String>(Time.seconds(0)) {
            @Override
            public long extractTimestamp(String element) {
                return Long.parseLong(element.split(",")[0].trim());
            }
        });

        // subtask中每个组的传入数据的时间间隔-->sum
        WindowedStream<Tuple2<String, Integer>, String, TimeWindow> sessionEventTimeWindow = lineWithWaterMark
                .map(value -> Tuple2.of(value.split(",")[1].trim(), 1), TypeInformation.of(new TypeHint<Tuple2<String, Integer>>() {
                }))
                .keyBy(value -> value.f0)
                .window(EventTimeSessionWindows.withGap(Time.seconds(5))); // 静态时间

        SingleOutputStreamOperator<Tuple2<String, Integer>> sumDataStream = sessionEventTimeWindow.sum(1);

        sumDataStream.print();
    }
}
