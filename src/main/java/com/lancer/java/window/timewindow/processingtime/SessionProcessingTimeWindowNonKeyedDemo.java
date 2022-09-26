package com.lancer.java.window.timewindow.processingtime;

import org.apache.flink.streaming.api.datastream.AllWindowedStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.ProcessingTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

public class SessionProcessingTimeWindowNonKeyedDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStreamSource<String> source = env.socketTextStream("bigdata01", 9999);

        AllWindowedStream<Integer, TimeWindow> sessionProcessingTimeWindowDataStream = source
                .map(Integer::parseInt)
                .windowAll(ProcessingTimeSessionWindows.withGap(Time.seconds(5))); // 静态时间
                /*.windowAll(ProcessingTimeSessionWindows.withDynamicGap(new SessionWindowTimeGapExtractor<Integer>() {
                    @Override
                    public long extract(Integer element) {
                        return element;
                    }
                }));*/

        SingleOutputStreamOperator<Integer> sumDataStream = sessionProcessingTimeWindowDataStream.sum(0);

        sumDataStream.print();

        env.execute();
    }
}
