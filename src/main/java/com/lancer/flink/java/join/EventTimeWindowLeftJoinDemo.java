package com.lancer.flink.java.join;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichCoGroupFunction;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;

import java.time.Duration;


public class EventTimeWindowLeftJoinDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        // 订单主表(订单id，订单总金额，订单状态)
        // 1000,o100,2000,已下单
        DataStreamSource<String> line1 = env.socketTextStream("bigdata01", 8888);
        SingleOutputStreamOperator<String> line1WithWaterMark = line1.assignTimestampsAndWatermarks(WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ZERO).withTimestampAssigner((element, recordTimestamp) -> Long.parseLong(element.split(",")[0])));

        SingleOutputStreamOperator<Tuple3<String, Double, String>> orderMainStream = line1WithWaterMark.map(new MapFunction<String, Tuple3<String, Double, String>>() {
            @Override
            public Tuple3<String, Double, String> map(String value) throws Exception {
                String[] fields = value.split(",");
                return Tuple3.of(fields[1], Double.parseDouble(fields[2]), fields[3]);
            }
        });

        // 订单明细表(订单主表id，分类，单价，数量)
        // 1000,o100,手机,500,2
        // 1111,o100,电脑,1000,1
        DataStreamSource<String> line2 = env.socketTextStream("bigdata01", 9999);
        SingleOutputStreamOperator<String> line2WithWaterMark = line2.assignTimestampsAndWatermarks(WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ZERO).withTimestampAssigner((element, recordTimestamp) -> Long.parseLong(element.split(",")[0])));
        SingleOutputStreamOperator<Tuple4<String, String, Double, Integer>> orderDetailStream = line2WithWaterMark.map(new MapFunction<String, Tuple4<String, String, Double, Integer>>() {
            @Override
            public Tuple4<String, String, Double, Integer> map(String value) throws Exception {
                String[] fields = value.split(",");
                return Tuple4.of(fields[1], fields[2], Double.parseDouble(fields[3]), Integer.parseInt(fields[4]));
            }
        });

        // left join后的数据
        // o100,手机,500,2,已下单
        // o100,电脑,1000,1,null

        // coGroup可以实现左外连接，右外连接等
        DataStream<Tuple5<String, String, Double, Integer, String>> resStream = orderDetailStream.coGroup(orderMainStream)
                .where(t1 -> t1.f0)
                .equalTo(t2 -> t2.f0)
                .window(TumblingEventTimeWindows.of(Time.seconds(5)))
                .apply(new RichCoGroupFunction<Tuple4<String, String, Double, Integer>, Tuple3<String, Double, String>, Tuple5<String, String, Double, Integer, String>>() {

                    // 只要窗口有数据，窗口触发就会调用，并且每一个key（join条件）都会调用
                    @Override
                    public void coGroup(Iterable<Tuple4<String, String, Double, Integer>> first, Iterable<Tuple3<String, Double, String>> second, Collector<Tuple5<String, String, Double, Integer, String>> out) throws Exception {

                        // 如果first有数据，second没有数据，可以实现join
                        // 如果first有数据，second没有数据，可以实现left join
                        // 如果first没有数据，second有数据，可以实现right join

                        boolean isJoined = false;
                        for (Tuple4<String, String, Double, Integer> left : first) {
                            for (Tuple3<String, Double, String> right : second) {
                                isJoined = true;
                                out.collect(Tuple5.of(left.f0, left.f1, left.f2, left.f3, right.f2));
                            }
                            if (!isJoined) {
                                out.collect(Tuple5.of(left.f0, left.f1, left.f2, left.f3, null));
                            }
                        }
                    }
                });

        resStream.print();

        env.execute();
    }
}
