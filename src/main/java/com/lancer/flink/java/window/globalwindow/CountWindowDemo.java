package com.lancer.flink.java.window.globalwindow;

import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;

/**
 * CountWindow是GlobalWindow的实现,是keyed Window，需要先keyBy，且并行度可以为多个
 */
public class CountWindowDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        env.setParallelism(2);

        DataStreamSource<String> source = env.socketTextStream("bigdata01", 9999);

        WindowedStream<Tuple2<String, Integer>, String, GlobalWindow> countWindowDataStream = source
                .map(value -> Tuple2.of(value, 1), TypeInformation.of(new TypeHint<Tuple2<String, Integer>>() {
                }))
                .keyBy(value -> value.f0)
                .countWindow(5);

        // 相同key的相加5个才输出，即每个分区中，每个分组达到5条数据才输出
        SingleOutputStreamOperator<Tuple2<String, Integer>> sumDataStream = countWindowDataStream.sum(1).setParallelism(10);

        sumDataStream.print();

        env.execute();
    }
}
