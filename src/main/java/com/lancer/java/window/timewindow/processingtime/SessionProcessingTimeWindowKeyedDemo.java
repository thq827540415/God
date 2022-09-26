package com.lancer.java.window.timewindow.processingtime;

import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.ProcessingTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

public class SessionProcessingTimeWindowKeyedDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);

        DataStreamSource<String> source = env.socketTextStream("bigdata01", 9999);

        // subtask中每个组的传入数据的时间间隔-->sum
        WindowedStream<Tuple2<String, Integer>, String, TimeWindow> sessionProcessingTimeWindowDataStream = source
                .map(value -> Tuple2.of(value, 1), TypeInformation.of(new TypeHint<Tuple2<String, Integer>>() {
                }))
                .keyBy(value -> value.f0)
                .window(ProcessingTimeSessionWindows.withGap(Time.seconds(5))); // 静态时间

        SingleOutputStreamOperator<Tuple2<String, Integer>> sumDataStream = sessionProcessingTimeWindowDataStream.sum(1);

        sumDataStream.print();

        env.execute();
    }
}
