package com.lancer.java.window.timewindow.eventtime;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.AllWindowedStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;

import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

/**
 *                            ***重点*** 看源码
 */
public class SlidingEventTimeWindowNonKeyedDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        test01(env);

        env.execute();
    }

    private static void test01(StreamExecutionEnvironment env) {

        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        // 窗口长度为10，5个长度滑动一次
        // 单位：ms
        // 1000,1
        // 2000,1
        // 为确保很小一部分的数据也能得到计算， 比如说0，那么将会在前面预留滑动长度个窗口(目的是为了让前面的数据在下次滑动前，有size大小的窗口可以计算)，比如这里，滑动长度为8s，那么会在此时间前预留8000ms的长度，所以在[-8000, 2000)有个窗口，下个窗口就是
        // lastStart = timestamp - (timestamp - offset + slid) % slid
        DataStreamSource<String> source = env.socketTextStream("bigdata01", 9999);

        SingleOutputStreamOperator<String> linesWithWaterMark = source.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<String>(Time.seconds(0)) {
            @Override
            public long extractTimestamp(String element) {
                return Long.parseLong(element.split(",")[0].trim());
            }
        });

        AllWindowedStream<Integer, TimeWindow> slidingEventTimeWindowDataStream = linesWithWaterMark
                .map(value -> Integer.parseInt(value.split(",")[1].trim()))
                .windowAll(SlidingEventTimeWindows.of(Time.seconds(10), Time.seconds(8)));// 传入windowAll方法中的参数为WindowAssigner（窗口分配器）

        SingleOutputStreamOperator<Integer> sumDataStream = slidingEventTimeWindowDataStream.sum(0);

        sumDataStream.print();
    }
}