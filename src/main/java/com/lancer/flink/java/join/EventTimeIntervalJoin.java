package com.lancer.flink.java.join;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.ProcessJoinFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;

import java.time.Duration;

/**
 * 按照时间范围进行join
 */
public class EventTimeIntervalJoin {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        // 订单主表(订单id，订单总金额，订单状态) 后输入
        // 0,o100,2000,已下单
        // 1999,o100,4000,已下单
        // 2000,o100,4000,已下单
        DataStreamSource<String> line1 = env.socketTextStream("bigdata01", 8888);
        SingleOutputStreamOperator<String> line1WithWaterMark = line1.assignTimestampsAndWatermarks(WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ZERO).withTimestampAssigner((element, recordTimestamp) -> Long.parseLong(element.split(",")[0])));

        SingleOutputStreamOperator<Tuple3<String, Double, String>> orderMainStream = line1WithWaterMark.map(new MapFunction<String, Tuple3<String, Double, String>>() {
            @Override
            public Tuple3<String, Double, String> map(String value) throws Exception {
                String[] fields = value.split(",");
                return Tuple3.of(fields[1], Double.parseDouble(fields[2]), fields[3]);
            }
        });

        // 订单明细表(订单主表id，分类，单价，数量) 先输入
        // 1000,o100,手机,500,2
        DataStreamSource<String> line2 = env.socketTextStream("bigdata01", 9999);
        SingleOutputStreamOperator<String> line2WithWaterMark = line2.assignTimestampsAndWatermarks(WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ZERO).withTimestampAssigner((element, recordTimestamp) -> Long.parseLong(element.split(",")[0])));
        SingleOutputStreamOperator<Tuple4<String, String, Double, Integer>> orderDetailStream = line2WithWaterMark.map(new MapFunction<String, Tuple4<String, String, Double, Integer>>() {
            @Override
            public Tuple4<String, String, Double, Integer> map(String value) throws Exception {
                String[] fields = value.split(",");
                return Tuple4.of(fields[1], fields[2], Double.parseDouble(fields[3]), Integer.parseInt(fields[4]));
            }
        });

        // 底层调用的是connect方法，两条流共享状态MapState
        SingleOutputStreamOperator<Tuple5<String, String, Double, Integer, String>> resStream = orderDetailStream.keyBy(t1 -> t1.f0)
                .intervalJoin(orderMainStream.keyBy(t2 -> t2.f0))
                .between(Time.seconds(-1), Time.seconds(1)) // 前一秒的数据和后一秒的数据能join上
                .upperBoundExclusive() // 前闭后开的，[lower, upper)
                .process(new ProcessJoinFunction<Tuple4<String, String, Double, Integer>, Tuple3<String, Double, String>, Tuple5<String, String, Double, Integer, String>>() {
                    @Override
                    public void processElement(Tuple4<String, String, Double, Integer> left, Tuple3<String, Double, String> right, Context ctx, Collector<Tuple5<String, String, Double, Integer, String>> out) throws Exception {
                        out.collect(Tuple5.of(left.f0, left.f1, left.f2, left.f3, right.f2));
                    }
                });

        resStream.print();

        env.execute();
    }
}
