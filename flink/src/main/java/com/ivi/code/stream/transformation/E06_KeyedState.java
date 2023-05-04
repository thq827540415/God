package com.ivi.code.stream.transformation;

import com.ivi.consts.CommonConstants;
import com.ivi.code.util.FlinkEnvUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.state.*;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.calcite.shaded.com.google.common.collect.Lists;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.util.*;

/**
 *                                      《状态总体概览》
 * 状态可以分为KeyedState和OperatorState，又可以分为RawState和ManagedState（是否由Flink管理）
 * <p>
 * State接口分为两类
 *      1. 一类由@PublicEvolving修饰的，提供给开发者使用的State接口，如ListState、ListState等
 *      2. 一类由Internal开头的接口，除了用于访问State中的数据外，还提供了访问内部运行时信息方法接口，如InternalKvState等
 * <p>
 * 内存中具体存储是使用接口OperatorStateStore和KeyedStateStore
 *      1. OperatorStateStore：是使用Map来保存状态的Map< String, PartitionableListState<?>>
 *              DefaultOperatorStateBackend#getListState(ListStateDescriptor)
 *                                          |
 *                                          V
 *              registeredOperatorStates.get(ListStateDescriptor#name)
 *                                          |
 *                                          V
 *              不存在的话 -> new PartitionableListState，存在 -> 直接返回PartitionableListState
 * <p>
 *      2. KeyedStateStore：是使用StateMapEntry来保存状态数据的，实际是HashTable
 *              DefaultKeyedStateStore#getPartitionedState(StateDescriptor)
 *                                          |
 *                                          V
 *              AbstractKeyedStateBackend#getPartitionedState(不是window，则默认namespace为VoidNamespace.INSTANCE) ->
 *                  getOrCreateKeyedState
 *                                          |
 *                                          V
 *              TtlStateFactory#createStateAndWrapWithTtlIfEnabled
 *                                          |
 *                                          V
 *              (KeyedStateFactory)HeapKeyedStateBackend#createInternalState ->
 *                  tryRegisterStateTable ->
 *                      StateTableFactory#newStateTable 生成CopyOnWriteStateTable ->
 *                          new StateMap 根据keyGroup数量生成CopyOnWriteStateMap ->
 *                              createStateMap() -> new StateMapEntry[128]
 *                                          |
 *                                          V
 *                     HeapKeyedStateBackend.StateFactory#createState
 *                                          |
 *                                          V
 *                               HeapValueState.create()
 *                                          |
 *                                          V
 *                                    HeapValueState
 * <p>
 *
 * KeyedStream中为每个key分配一个独立的状态
 * <p>
 * KeyedState:<p>
 *      1. ValueState：用于保存一个值. eg: ValueState< Map< String, Int>> <==> MapState< String, Int><p>
 *      2. ListState：用于保存一个列表<p>
 *      3. MapState：用于保存一个Map<p>
 *      4. ReducingState<p>
 *      5. AggregatingState
 */
@Slf4j
public class E06_KeyedState {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        DataStreamSource<String> source = env.socketTextStream(CommonConstants.NC_HOST, 9999);

        // doValueState(source);
        doListState(source);
        // doMapState(source);

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
                        // 与StreamGroupedReduceOperator类中类似
                        new KeyedProcessFunction<String, Tuple2<String, Integer>, Tuple2<String, Integer>>() {

                            private transient ValueState<Integer> valueState;

                            @Override
                            public void open(Configuration parameters) throws Exception {
                                // 设置单个状态的超时时间，（只有keyed state才能设置超时时间）
                                // UpdateType表明了过期时间什么时候更新
                                // StateVisibility表明了过期的状态，是否还能被访问
                                StateTtlConfig ttlConfig = StateTtlConfig.newBuilder(Time.seconds(60))
                                        // 覆盖builder中传入的时间
                                        .setTtl(Time.seconds(120))

                                        // 默认使用的是eventTime
                                        // .useProcessingTime()


                                        // 1. 默认重置ttl策略
                                        // .updateTtlOnCreateAndWrite()
                                        // 2. 关闭ttl，状态永不过期
                                        // .setUpdateType(StateTtlConfig.UpdateType.Disabled)
                                        // 3. 数据只要被Read或者Write，则重置ttl
                                        // .updateTtlOnReadAndWrite()


                                        // 1. 数据只要超时就不能使用了
                                        // .neverReturnExpired()
                                        // .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
                                        // 2. 数据只要过期还没被cleanUp（开个线程），就还可以使用
                                        // .returnExpiredIfNotCleanedUp()


                                        // 1. 增量清理，为true时每一条状态数据被访问，则会检查1000条数据是否过期
                                        //    只用于HashMapStateBackend
                                        .cleanupIncrementally(1000, true)
                                        // 2. 全量清理，checkpoint的时候，只保存未过期的状态数据，但是并不会清理算子本地的状态数据
                                        // .cleanupFullSnapshot()
                                        // 3. 在rocksdb的compact机制中添加过期数据过滤器，以在compact过程中清理过期的状态数据
                                        //    只用于EmbeddedRocksDBStateBackend
                                        // .cleanupInRocksdbCompactFilter(1000)


                                        // 禁用默认后台清理策略
                                        // 如果StateBackend支持garbage collected in the background，则会周期性后台清理
                                        .disableCleanupInBackground()
                                        .build();

                                // 获取永不过期的ttl
                                // StateTtlConfig disabled = StateTtlConfig.DISABLED;

                                ValueStateDescriptor<Integer> valueStateDescriptor = new ValueStateDescriptor<>(
                                        "value state descriptor",
                                        new TypeHint<Integer>() {
                                        }.getTypeInfo());
                                // 开启ttl
                                valueStateDescriptor.enableTimeToLive(ttlConfig);

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
     *      uid100 view
     *      uid102 addCart
     *      uid100 view
     *      uid100 pay
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

                                // ----------------------------------------------------------------
                                // 会使用UserFacingListState<>包装一下，避免返回值为null
                                // EmptyList没有重写AbstractList的add方法
                                // ----------------------------------------------------------------
                                List<String> list = Lists.newArrayList(listState.get().iterator());

                                // 只保存最近3次行为
                                if (list.size() >= 3) {
                                    list.remove(0);
                                }
                                list.add(value.f1);
                                listState.update(list);
                                out.collect(Tuple2.of(value.f0, list));
                            }
                        })
                .print();
    }

    /**
     * 求每个市的总金额
     *      辽宁省 大连市 1000
     *      河北省 廊坊市 2000
     *      河北省 廊坊市 2000
     *      辽宁省 沈阳市 1000
     */
    private static void doMapState(DataStreamSource<String> source) {
        source
                .map(
                        line -> {
                            String[] split = line.split("\\s+");
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

                                // 使用MapState
                                Integer money = mapState.get(value.f0);
                                if (!Objects.isNull(money)) {
                                    value.f1 += money;
                                }
                                mapState.put(value.f0, value.f1);
                                String[] split1 = value.f0.split(",");
                                out.collect(Tuple3.of(split1[0], split1[1], value.f1));


                                // 使用ListState
                                Integer integer = valueState.value();
                                if (!Objects.isNull(integer)) {
                                    value.f1 += integer;
                                }
                                valueState.update(value.f1);
                                String[] split2 = value.f0.split(",");
                                out.collect(Tuple3.of(split2[0], split2[1], value.f1));
                            }
                        })
                .print();
    }
}
