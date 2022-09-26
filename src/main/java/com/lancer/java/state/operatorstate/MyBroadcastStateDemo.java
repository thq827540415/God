package com.lancer.java.state.operatorstate;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;

/**
 * 带有状态的广播
 */
public class MyBroadcastStateDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.enableCheckpointing(10000);

        env.setParallelism(2);

        // 维度数据，每台机器上都要有，如果没有就要查数据库
        // 1,图书,INSERT
        // 2,手机,INSERT
        // 3,家具,INSERT
        // 1,少儿读物,UPDATE
        // 1,少儿读物,DELETE
        SingleOutputStreamOperator<Tuple3<String, String, String>> categoryStream = env
                .socketTextStream("bigdata01", 9998)
                .map(new MapFunction<String, Tuple3<String, String, String>>() {
                    @Override
                    public Tuple3<String, String, String> map(String value) throws Exception {
                        String[] fields = value.split(",");
                        return Tuple3.of(fields[0], fields[1], fields[2]);
                    }
                });

        MapStateDescriptor<String, String> categoryMapStateDescriptor = new MapStateDescriptor<>("category-broadcast-state", String.class, String.class);
        // 这里的MapState不是keyedState中的MapState。先将流中数据广播出去，再将流以MapState即KV的形式在下游存储起来
        // 和broadcast()不一样，broadcast()只广播数据
        BroadcastStream<Tuple3<String, String, String>> broadcastStream = categoryStream.broadcast(categoryMapStateDescriptor);

        // 事实数据，订单数据库
        // 1,50        --> 单位块
        // 2,2000
        SingleOutputStreamOperator<Tuple2<String, Double>> orderStream = env
                .socketTextStream("bigdata01", 9999)
                .map(new MapFunction<String, Tuple2<String, Double>>() {
                    @Override
                    public Tuple2<String, Double> map(String value) throws Exception {
                        String[] fields = value.split(",");
                        return Tuple2.of(fields[0].trim(), Double.parseDouble(fields[1].trim()));
                    }
                });

        // 关联维度后，输出结果
        // 1,50,图书
        // 事实流connect维度流，connect后可以共享状态，用事实关联维度
        SingleOutputStreamOperator<Tuple3<String, String, Double>> resStream = orderStream
                .connect(broadcastStream)
                .process(new BroadcastProcessFunction<Tuple2<String, Double>, Tuple3<String, String, String>, Tuple3<String, String, Double>>() {

                    /**
                     * 当一条数据来时，只调用一次
                     * @param value
                     * @param ctx
                     * @param out
                     * @throws Exception
                     */
                    @Override
                    public void processElement(Tuple2<String, Double> value, ReadOnlyContext ctx, Collector<Tuple3<String, String, Double>> out) throws Exception {
                        String cid = value.f0;
                        Double money = value.f1;
                        // 关联
                        ReadOnlyBroadcastState<String, String> categoryState = ctx.getBroadcastState(categoryMapStateDescriptor);
                        System.out.println("subtask: " + getRuntimeContext().getIndexOfThisSubtask() + " , " + value + " , " + categoryState);

                        String name = categoryState.get(cid);
                        if (name == null) {
                            name = "未知";
                        }
                        out.collect(Tuple3.of(cid, name, money));
                    }

                    /**
                     * 处理广播的数据，维度流
                     * 当一条数据来时，有几个subtask就执行几次,并同时更新对应机器上的那个状态
                     * @param value
                     * @param ctx
                     * @param out
                     * @throws Exception
                     */
                    @Override
                    public void processBroadcastElement(Tuple3<String, String, String> value, Context ctx, Collector<Tuple3<String, String, Double>> out) throws Exception {
                        String cid = value.f0;
                        String name = value.f1;
                        String type = value.f2;

                        BroadcastState<String, String> categoryState = ctx.getBroadcastState(categoryMapStateDescriptor);

                        // 一个TM中的多个subtask共享一个BroadcastState，节省资源
                        System.out.println("subtask: " + getRuntimeContext().getIndexOfThisSubtask() + " , " + value + " , " + categoryState + " , " + categoryState.get(cid));

                        if (type.equals("DELETE")) {
                            categoryState.remove(cid);
                        } else {
                            categoryState.put(cid, name);
                        }
                    }
                });

        resStream.print();

        env.execute();
    }
}
