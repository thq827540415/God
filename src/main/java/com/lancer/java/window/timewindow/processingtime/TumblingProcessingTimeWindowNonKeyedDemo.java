package com.lancer.java.window.timewindow.processingtime;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.AllWindowedStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

/**
 * 滚动窗口：滚动的时间和窗口的长度相等
 *
 * 可以按照EventTime划分，也可以按照ProcessingTime划分（主要）
 *
 * 也分为keyed window 和 non-keyed window
 */
public class TumblingProcessingTimeWindowNonKeyedDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        DataStreamSource<String> source = env.socketTextStream("bigdata01", 9999);

        // 不keyBy，使用processingTime
        /*source
                .map(Integer::parseInt)
                .timeWindowAll(Time.seconds(5));*/ // 过时了

        AllWindowedStream<Integer, TimeWindow> tumblingProcessingTimeWindowDataStream = source
                .map(Integer::parseInt)
                // 从程序启动开始，每5s统计一次
                // 传入windowAll方法中的参数为WindowAssigner（窗口分配器）
                .windowAll(TumblingProcessingTimeWindows.of(Time.seconds(5)));

        SingleOutputStreamOperator<Integer> sumDataStream = tumblingProcessingTimeWindowDataStream.sum(0);

        sumDataStream.print();

        env.execute();
    }
}
