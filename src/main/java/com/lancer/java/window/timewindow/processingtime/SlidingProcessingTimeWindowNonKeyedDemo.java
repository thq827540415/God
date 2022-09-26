package com.lancer.java.window.timewindow.processingtime;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.AllWindowedStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

public class SlidingProcessingTimeWindowNonKeyedDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        DataStreamSource<String> source = env.socketTextStream("bigdata01", 9999);

        // 不keyBy，使用processingTime
        /*source
                .map(Integer::parseInt)
                .timeWindowAll(Time.seconds(10), Time.seconds(5));*/ // 过时了

        AllWindowedStream<Integer, TimeWindow> slidingProcessingTimeWindowDataStream = source
                .map(Integer::parseInt)
                .windowAll(SlidingProcessingTimeWindows.of(Time.seconds(10), Time.seconds(5)));// 传入windowAll方法中的参数为WindowAssigner（窗口分配器）

        SingleOutputStreamOperator<Integer> sumDataStream = slidingProcessingTimeWindowDataStream.sum(0);

        sumDataStream.print();

        env.execute();
    }
}
