package com.lancer.java.window.timewindow.processingtime;

import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

/**
 * 应用于每个分组
 */
public class SlidingProcessingTimeWindowKeyedDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        DataStreamSource<String> source = env.socketTextStream("bigdata01", 9999);

        // keyBy，使用processingTime
        /*source
                .map(value -> Tuple2.of(value, 1), TypeInformation.of(new TypeHint<Tuple2<String, Integer>>() {
                }))
                .keyBy(value -> value.f0)
                .timeWindow(Time.seconds(10), Time.seconds(5));*/ // 过时了

        WindowedStream<Tuple2<String, Integer>, String, TimeWindow> slidingProcessingTimeWindowDataStream = source
                .map(value -> Tuple2.of(value, 1), TypeInformation.of(new TypeHint<Tuple2<String, Integer>>() {
                }))
                .keyBy(value -> value.f0)
                .window(SlidingProcessingTimeWindows.of(Time.seconds(10), Time.seconds(5)));// 传入windowAll方法中的参数为WindowAssigner（窗口分配器）

        SingleOutputStreamOperator<Tuple2<String, Integer>> sumDataStream = slidingProcessingTimeWindowDataStream.sum(1);

        sumDataStream.print();

        env.execute();
    }
}
