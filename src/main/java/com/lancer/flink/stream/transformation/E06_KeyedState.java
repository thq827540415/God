package com.lancer.flink.stream.transformation;

import com.lancer.FlinkEnvUtils;
import com.lancer.consts.UsualConsts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.functions.util.PrintSinkOutputWriter;
import org.apache.flink.api.common.state.*;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.common.typeutils.base.IntSerializer;
import org.apache.flink.api.common.typeutils.base.StringSerializer;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * KeyedStream中为每个key分配一个独立的状态
 * <p>
 * KeyedState:
 * 1. ValueState：用于保存一个值. eg: ValueState`<`Map`<`String, Int>> == MapState`<`String, Int>
 * 2. ListState：用于保存一个列表
 * 3. MapState：用于保存一个Map
 * 4. ReducingState
 * 5. AggregatingState
 */
public class E06_KeyedState {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();
        env.setParallelism(2);

        DataStreamSource<String> source = env.socketTextStream(UsualConsts.NC_HOST, 9999);

        // doListState(source);
        // doValueState(source);
        doMapState(source);

        env.execute(E06_KeyedState.class.getSimpleName());
    }


    /**
     * WordCount
     */
    private static void doValueState(DataStreamSource<String> source) {
        source
                .map(
                        line -> {
                            String[] split = line.split("\\s+");
                            return Tuple2.of(split[0], Integer.parseInt(split[1]));
                        }, new TypeHint<Tuple2<String, Integer>>() {
                        }.getTypeInfo())
                .keyBy(t -> t.f0)
                .process(
                        // StreamGroupedReduceOperator类中类似
                        new KeyedProcessFunction<String, Tuple2<String, Integer>, Tuple2<String, Integer>>() {

                            /**
                             * 每个key分配一个独立的状态，即使他们处于同一个分区
                             */
                            private transient ValueState<Integer> valueState;

                            @Override
                            public void open(Configuration parameters) throws Exception {
                                // 设置单个状态的超时时间，（只有keyed state才能设置超时时间）
                                StateTtlConfig ttlConfig = StateTtlConfig.newBuilder(Time.seconds(60))
                                        // 默认存活时间更新类型
                                        // .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
                                        // 关闭TTL，状态永不过期
                                        // .setUpdateType(StateTtlConfig.UpdateType.Disabled)
                                        .setUpdateType(StateTtlConfig.UpdateType.OnReadAndWrite)
                                        // 数据只要超时就不能使用了
                                        .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
                                        // 数据只要还没被cleanUp（开个线程），就还可以使用
                                        // .setStateVisibility(StateTtlConfig.StateVisibility.ReturnExpiredIfNotCleanedUp)
                                        .build();

                                ValueStateDescriptor<Integer> valueStateDescriptor = new ValueStateDescriptor<>(
                                        "value state descriptor",
                                        new TypeHint<Integer>() {
                                        }.getTypeInfo());
                                // 开启TTL
                                valueStateDescriptor.enableTimeToLive(ttlConfig);

                                // 初始化或者是恢复状态，底层调用的是keyedStateBackend.getPartitionedState(...)
                                valueState = getRuntimeContext().getState(valueStateDescriptor);
                            }

                            @Override
                            public void processElement(Tuple2<String, Integer> value,
                                                       Context ctx,
                                                       Collector<Tuple2<String, Integer>> out) throws Exception {
                                Integer currentValue = valueState.value();
                                if (!Objects.isNull(currentValue)) {
                                    value.f1 += currentValue;
                                }
                                valueState.update(value.f1);
                                out.collect(value);
                            }
                        })
                .print();
    }

    /**
     * 将用户最近5次行为保存下来
     * uid100,view
     * uid102,addCart
     * uid100,view
     * uid100,pay
     */
    private static void doListState(DataStreamSource<String> source) {
        source
                .filter(StringUtils::isNotBlank)
                .map(
                        line -> {
                            String[] split = line.split("\\s+");
                            return Tuple2.of(split[0], split[1]);
                        }, Types.TUPLE(Types.STRING, Types.STRING))
                .keyBy(t -> t.f0)
                .process(
                        new KeyedProcessFunction<String, Tuple2<String, String>, Tuple2<String, List<String>>>() {

                            private transient ListState<String> listState;

                            @Override
                            public void open(Configuration parameters) throws Exception {
                                ListStateDescriptor<String> listStateDescriptor = new ListStateDescriptor<>(
                                        "list state descriptor",
                                        Types.STRING);
                                listState = getRuntimeContext().getListState(listStateDescriptor);
                            }

                            @Override
                            public void processElement(Tuple2<String, String> value,
                                                       Context ctx,
                                                       Collector<Tuple2<String, List<String>>> out) throws Exception {
                                String uid = value.f0;
                                String event = value.f1;
                                // 方法一
                                listState.add(event); // 不用判断是否为空，因为add方法底层会帮每一个key都new ArrayList<>();
                                out.collect(Tuple2.of(uid, (List<String>) listState.get()));

                                // 方法二
                                // 上面可能会导致list过多
                                /*listState.add(event);
                                List<String> list = (List<String>) listState.get();
                                if (list.size() > 5) {
                                    list.remove(0);
                                }
                                // 因为是引用类型，是在第一次add的时候，底层使用了new ArrayList<>()方法,
                                // 并将该新建的list放入namespace对应的StateTable中，所以不用update，当你自己new一个List的时候，
                                // 需要update -> 因为数据都在外部的list中，使用update方法，放入内部的list
                                // update方法，底层是将传入的List放入update方法中的newStateList中
                                // （List<V> newStateList = new ArrayList<>();），
                                // 然后再将update中的newStateList放入namespace对应的StateTable中
                                // listState.update(list);
                                return Tuple2.of(uid, list);*/

                                // 方法三 */
                                /*Iterable<String> events = listState.get();
                                if (!events.iterator().hasNext()) {
                                    // ArrayList默认的长度为10
                                    events = new ArrayList<>(5);
                                }
                                List<String> list = (List<String>) events;
                                if (list.size() == 5) {
                                    list.remove(0);
                                }
                                list.add(event);
                                listState.update(list);
                                return Tuple2.of(uid, list);*/

                                /* 方法四 */
                                /*List<String> list = (List<String>) listState.get();
                                if (list.size() == 0) {
                                    list = new ArrayList<>(5);
                                } else if (list.size() == 5) {
                                    list.remove(0);
                                }
                                list.add(event);
                                // 数据还未放在stateTable中，故需要update
                                listState.update(list);
                                return Tuple2.of(uid, list);*/
                            }
                        })
                .print();
    }

    /**
     * 求每个市的总金额
     * 辽宁省 大连市 1000
     * 河北省 廊坊市 2000
     * 河北省 廊坊市 2000
     * 辽宁省 沈阳市 1000
     */
    private static void doMapState(DataStreamSource<String> source) {
        source
                .map(
                        value -> {
                            String[] split = value.split("\\s+");
                            return Tuple2.of(split[0] + "," + split[1], Integer.parseInt(split[2]));
                        }, TypeInformation.of(new TypeHint<Tuple2<String, Integer>>() {
                        }))
                .keyBy(t -> t.f0)
                .process(
                        new KeyedProcessFunction<String, Tuple2<String, Integer>, Tuple3<String, String, Integer>>() {

                            private transient MapState<String, Integer> mapState;

                            private transient ValueState<Integer> valueState;

                            @Override
                            public void open(Configuration parameters) throws Exception {
                                MapStateDescriptor<String, Integer> mapStateDescriptor =
                                        new MapStateDescriptor<>(
                                                "map state descriptor",
                                                String.class,
                                                Integer.class);
                                mapState = getRuntimeContext().getMapState(mapStateDescriptor);


                                ValueStateDescriptor<Integer> valueStateDescriptor = new ValueStateDescriptor<>(
                                        "value",
                                        Integer.class);
                                valueState = getRuntimeContext().getState(valueStateDescriptor);
                            }

                            @Override
                            public void processElement(
                                    Tuple2<String, Integer> value,
                                    Context ctx,
                                    Collector<Tuple3<String, String, Integer>> out) throws Exception {

                                /*Integer money = mapState.get(value.f0);
                                if (!Objects.isNull(money)) {
                                    value.f1 += money;
                                }
                                mapState.put(value.f0, value.f1);
                                String[] split = value.f0.split(",");
                                out.collect(Tuple3.of(split[0], split[1], value.f1));*/


                                Integer integer = valueState.value();
                                if (!Objects.isNull(integer)) {
                                    value.f1 += integer;
                                }
                                valueState.update(value.f1);
                                String[] split = value.f0.split(",");
                                out.collect(Tuple3.of(split[0], split[1], value.f1));
                            }
                        })
                .print();
    }
}
