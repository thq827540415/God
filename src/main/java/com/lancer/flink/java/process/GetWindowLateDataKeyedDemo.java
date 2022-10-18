package com.lancer.flink.java.process;

import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.OutputTag;

import java.util.concurrent.TimeUnit;

public class GetWindowLateDataKeyedDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        test01(env);

        env.execute();
    }

    // 老版API
    private static void test01(StreamExecutionEnvironment env) {

        env.setParallelism(2);

        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        // 1000,spark
        // 2000,hadoop
        // 5000,spark
        // 3000,hadoop --> 迟到数据默认被抛弃了
        DataStreamSource<String> source = env.socketTextStream("bigdata01", 9999);

        SingleOutputStreamOperator<String> linesWithWaterMark = source.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<String>(Time.seconds(0)) { // 达到WaterMark时，延迟Time.seconds(s)后触发
            @Override
            public long extractTimestamp(String element) {
                return Long.parseLong(element.split(",")[0].trim());
            }
        });

        OutputTag<Tuple2<String, Integer>> lateDataTag = new OutputTag<>("lateData", TypeInformation.of(new TypeHint<Tuple2<String, Integer>>() {
        }));

        WindowedStream<Tuple2<String, Integer>, String, TimeWindow> tumblingEventTimeWindow = linesWithWaterMark
                .map(value -> Tuple2.of(value.split(",")[1].trim(), 1), TypeInformation.of(new TypeHint<Tuple2<String, Integer>>() {
                }))
                .keyBy(value -> value.f0)
                .window(TumblingEventTimeWindows.of(Time.of(5, TimeUnit.SECONDS)))
                .sideOutputLateData(lateDataTag); // 将迟到的数据，在侧流中输出

        SingleOutputStreamOperator<Tuple2<String, Integer>> sumDataStream = tumblingEventTimeWindow.sum(1);
        sumDataStream.print();

        sumDataStream.getSideOutput(lateDataTag).print("迟到的数据：");
    }
}
