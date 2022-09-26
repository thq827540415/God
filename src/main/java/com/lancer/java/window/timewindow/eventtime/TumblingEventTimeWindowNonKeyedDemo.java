package com.lancer.java.window.timewindow.eventtime;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.AllWindowedStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

import java.text.SimpleDateFormat;

public class TumblingEventTimeWindowNonKeyedDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        test01(env);

        // test02(env);

        env.execute();
    }

    // 老版API
    public static void test01(StreamExecutionEnvironment env) {

        // 设置时间类型，默认就是EventTime
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        // 2021-07-04 15:00:00,1
        // 2021-07-04 15:00:02,2
        // 2021-07-04 15:00:04,4
        DataStreamSource<String> source = env.socketTextStream("bigdata01", 9999);

        // 提取数据中的时间作为EventTime，然后根据EventTime划分窗口
        // 生成一个WaterMark，数据还是原来样子
        // 超出WaterMark，触发窗口计算
        SingleOutputStreamOperator<String> linesWithWaterMark = source
                .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<String>(Time.seconds(0)) { // 乱序迟到的时间戳提取器, Time.seconds(0)为：数据不延迟

                    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    // private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

                    @Override
                    public long extractTimestamp(String element) {
                        try {
                            return sdf.parse(element.split(",")[0].trim()).getTime();
                        } catch (Exception e) {
                            return System.currentTimeMillis();
                        }
                    }
                });

        AllWindowedStream<Integer, TimeWindow> tumblingEventTimeWindowDataStream = linesWithWaterMark
                .map(value -> Integer.parseInt(value.split(",")[1].trim()))
                .timeWindowAll(Time.seconds(5)); // 支持毫秒级触发
        /**
         *   5s的EventTime，只有5s的间隔 --> 2021-07-04 15:00:00,1
         *                                            2021-07-04 15:00:02,2
         *                                            2021-07-04 15:00:05,4
         *                                        到达初始时间 + 5s的时间，才会触发,输出相加结果为1 + 2 = 3
         *                                        Time.seconds(s) --> 到达 n * s时间才会触发，如s = 5, 那么1 * 5, 2 * 5
         */

        SingleOutputStreamOperator<Integer> sumDataStream = tumblingEventTimeWindowDataStream.sum(0);

        sumDataStream.print();
    }

    // 老API
    public static void test02(StreamExecutionEnvironment env) {

        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        // 精确到毫秒的时间戳
        // 窗口的起始时间，结束时间是对齐的，必须是窗口长度的整数倍 eg：1625389200002 -> [1625389200000, 1625389205000)
        //                                                                               1625389203124 -> [1625389200000, 1625389205000)
        //                                                                               1625389206322 -> [1625389205000, 1625389210000)
        // 由上例可知，都是在每个固定长度的时间内的，并不会出现，[1625389200003, 1625389205003)
        // 1625389200000,1 --> 第0s
        // 1625389202000,2 --> 第2s
        // 1625389204000,4 --> 第4s
        // 1625389204999,5 --> 第4.999s ,此时会触发窗口计算了，1625389205000就是新的开始了
        DataStreamSource<String> source = env.socketTextStream("bigdata01", 9999);

        SingleOutputStreamOperator<String> linesWithWaterMark = source
                .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<String>(Time.seconds(0)) {
                    @Override
                    public long extractTimestamp(String element) {
                        return Long.parseLong(element.split(",")[0].trim());
                    }
                });

        AllWindowedStream<Integer, TimeWindow> tumblingEventTimeWindowDataStream1 = linesWithWaterMark
                .map(value -> Integer.parseInt(value.split(",")[1].trim()))
                .timeWindowAll(Time.seconds(5)); // 支持毫秒级触发

        SingleOutputStreamOperator<Integer> sumDataStream1 = tumblingEventTimeWindowDataStream1.sum(0);

        sumDataStream1.print();
    }
}
