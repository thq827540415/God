package com.lancer.flink.java.window.globalwindow;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.AllWindowedStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;

/**
 * CountWindowAll是GlobalWindow的实现,是Non-keyed Window，不需要先keyBy，且并行度只能为1
 */
public class CountWindowAllDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        DataStreamSource<String> source = env.socketTextStream("bigdata01", 9999);

        AllWindowedStream<Integer, GlobalWindow> countWindowAllDataStream = source
                .map(Integer::parseInt)
                .countWindowAll(5);// 5条数据生成一个窗口

        SingleOutputStreamOperator<Integer> sumDataStream = countWindowAllDataStream.sum(0);

        sumDataStream.print();

        env.execute();
    }
}
