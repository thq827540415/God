package com.lancer.flink.java.window.timewindow.eventtime;

import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.AllWindowedStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

public class SessionEventTimeWindowNonKeyedDemo {
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

        AllWindowedStream<Integer, TimeWindow> sessionEventTimeWindow = lineWithWaterMark
                .map(Integer::parseInt)
                .windowAll(EventTimeSessionWindows.withGap(Time.seconds(5))); // 静态时间


        SingleOutputStreamOperator<Integer> sumDataStream = sessionEventTimeWindow.sum(0);

        sumDataStream.print();
    }
}
