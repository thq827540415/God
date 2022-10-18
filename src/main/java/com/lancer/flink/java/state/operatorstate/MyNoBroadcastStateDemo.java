package com.lancer.flink.java.state.operatorstate;

import org.apache.flink.api.common.functions.AbstractRichFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.CoFlatMapFunction;
import org.apache.flink.util.Collector;

/**
 * 不使用Broadcast广播状态
 */
public class MyNoBroadcastStateDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        env.enableCheckpointing(30000);

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

        DataStream<Tuple3<String, String, String>> broadcastStream = categoryStream.broadcast();

        // 事实数据，订单数据库
        // 1,50        --> 单位块
        // 2,2000
        SingleOutputStreamOperator<Tuple2<String, Double>> orderStream = env
                .socketTextStream("bigdata01", 9999)
                .map(new MapFunction<String, Tuple2<String, Double>>() {
                    @Override
                    public Tuple2<String, Double> map(String value) throws Exception {
                        String[] fields = value.split(",");
                        if (fields[0].equals("error")) {
                            throw new RuntimeException("test error");
                        }
                        return Tuple2.of(fields[0].trim(), Double.parseDouble(fields[1].trim()));
                    }
                });

        // 关联维度后，输出结果
        // 1,50,图书
        // 事实流connect维度流，connect后可以共享状态，用事实关联维度
        SingleOutputStreamOperator<Tuple3<String, String, Double>> resStream = orderStream
                .connect(broadcastStream)
                /*.flatMap(new CoFlatMapFunction<Tuple2<String, Double>, Tuple3<String, String, String>, Tuple3<String, String, Double>>() {

                    private final Map<String, String> map = new HashMap<>();

                    @Override
                    public void flatMap1(Tuple2<String, Double> value, Collector<Tuple3<String, String, Double>> out) throws Exception {
                        String cid = value.f0;
                        Double money = value.f1;
                        String name = map.get(cid);
                        if (name == null) {
                            name = "未知";
                        }
                        out.collect(Tuple3.of(cid, name, money));
                    }

                    @Override
                    public void flatMap2(Tuple3<String, String, String> value, Collector<Tuple3<String, String, Double>> out) throws Exception {
                        String cid = value.f0;
                        String name = value.f1;
                        String type = value.f2;

                        if (type.equals("DELETE")) {
                            map.remove(cid);
                        } else {
                            map.put(cid, name);
                        }
                    }
                })*/
                .flatMap(new MyBroadcastState2());

        resStream.print();

        env.execute();
    }

    /* 不能使用MapState */
    private static class MyBroadcastState extends AbstractRichFunction implements CoFlatMapFunction<Tuple2<String, Double>, Tuple3<String, String, String>, Tuple3<String, String, Double>> {

        // Keyed state can only be used on a 'keyed stream'
        private transient MapState<String, String> mapState;

        @Override
        public void open(Configuration parameters) throws Exception {
            MapStateDescriptor<String, String> mapStateDescriptor = new MapStateDescriptor<>("broadcast-test", String.class, String.class);
            mapState = getRuntimeContext().getMapState(mapStateDescriptor);
        }

        @Override
        public void flatMap1(Tuple2<String, Double> value, Collector<Tuple3<String, String, Double>> out) throws Exception {
            String cid = value.f0;
            Double money = value.f1;
            String name = mapState.get(cid);
            if (name == null) {
                name = "未知";
            }
            out.collect(Tuple3.of(cid, name, money));
        }

        @Override
        public void flatMap2(Tuple3<String, String, String> value, Collector<Tuple3<String, String, Double>> out) throws Exception {
            String cid = value.f0;
            String name = value.f1;
            String type = value.f2;

            if (type.equals("DELETE")) {
                mapState.remove(cid);
            } else {
                mapState.put(cid, name);
            }
        }
    }

    /* 使用BroadcastState可以 */
    private static class MyBroadcastState2 implements CoFlatMapFunction<Tuple2<String, Double>, Tuple3<String, String, String>, Tuple3<String, String, Double>>, CheckpointedFunction {

        private transient BroadcastState<String, String> broadcastState;

        @Override
        public void flatMap1(Tuple2<String, Double> value, Collector<Tuple3<String, String, Double>> out) throws Exception {
            String cid = value.f0;
            Double money = value.f1;
            String name = broadcastState.get(cid);
            if (name == null) {
                name = "未知";
            }
            out.collect(Tuple3.of(cid, name, money));
        }

        @Override
        public void flatMap2(Tuple3<String, String, String> value, Collector<Tuple3<String, String, Double>> out) throws Exception {
            String cid = value.f0;
            String name = value.f1;
            String type = value.f2;

            if (type.equals("DELETE")) {
                broadcastState.remove(cid);
            } else {
                broadcastState.put(cid, name);
            }
        }

        @Override
        public void snapshotState(FunctionSnapshotContext context) throws Exception {

        }

        @Override
        public void initializeState(FunctionInitializationContext context) throws Exception {
            MapStateDescriptor<String, String> mapStateDescriptor = new MapStateDescriptor<>("broadcast-test", String.class, String.class);
            broadcastState = context.getOperatorStateStore().getBroadcastState(mapStateDescriptor);
        }
    }
}
