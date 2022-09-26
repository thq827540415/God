package com.lancer.java.join;

import org.apache.flink.api.common.functions.JoinFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.tuple.Tuple5;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;


public class ProcessingTimeWindowJoinDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        // 订单主表(订单id，订单总金额，订单状态)
        // o100,2000,已下单
        DataStreamSource<String> line1 = env.socketTextStream("bigdata01", 8888);
        SingleOutputStreamOperator<Tuple3<String, Double, String>> orderMainStream = line1.map(new MapFunction<String, Tuple3<String, Double, String>>() {
            @Override
            public Tuple3<String, Double, String> map(String value) throws Exception {
                String[] fields = value.split(",");
                return Tuple3.of(fields[0], Double.parseDouble(fields[1]), fields[2]);
            }
        });

        // 订单明细表(订单主表id，分类，单价，数量)
        // o100,手机,500,2
        // o100,电脑,1000,1
        DataStreamSource<String> line2 = env.socketTextStream("bigdata01", 9999);
        SingleOutputStreamOperator<Tuple4<String, String, Double, Integer>> orderDetailStream = line2.map(new MapFunction<String, Tuple4<String, String, Double, Integer>>() {
            @Override
            public Tuple4<String, String, Double, Integer> map(String value) throws Exception {
                String[] fields = value.split(",");
                return Tuple4.of(fields[0], fields[1], Double.parseDouble(fields[2]), Integer.parseInt(fields[3]));
            }
        });

        // 两条流进行join,这里是内连接,join方法的底层调用的也是coGroup方法
        // join后的数据
        // o100,已下单,手机,500,2
        DataStream<Tuple5<String, String, String, Double, Integer>> joinedStream = orderMainStream.join(orderDetailStream)
                // 这里的两个条件，实质上是对传入的两个流进行union，然后用KeyedStream包装起来，然后再调用窗口
                .where(t -> t.f0) // 第一个流中条件
                .equalTo(t -> t.f0) // 第二个流中的条件
                .window(TumblingProcessingTimeWindows.of(Time.seconds(10)))
                .apply(new JoinFunction<Tuple3<String, Double, String>, Tuple4<String, String, Double, Integer>, Tuple5<String, String, String, Double, Integer>>() {
                    /**
                     * 在同一窗口内，并且条件相同
                     * @param first
                     * @param second
                     * @return
                     * @throws Exception
                     */
                    @Override
                    public Tuple5<String, String, String, Double, Integer> join(Tuple3<String, Double, String> first, Tuple4<String, String, Double, Integer> second) throws Exception {
                        return Tuple5.of(first.f0, first.f2, second.f1, second.f2, second.f3);
                    }
                });

        joinedStream.print();

        env.execute();
    }
}
