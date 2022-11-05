package com.lancer.flink.stream.transformation;

import com.lancer.util.FlinkEnvUtils;
import com.lancer.consts.UsualConsts;
import org.apache.flink.api.common.functions.util.PrintSinkOutputWriter;
import org.apache.flink.api.common.state.*;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.common.typeutils.base.LongSerializer;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.typeutils.TupleTypeInfo;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.TimeDomain;
import org.apache.flink.streaming.api.TimerService;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.streaming.api.functions.co.CoProcessFunction;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.util.Collector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 每个subtask中持有一份独立的状态，当失败恢复后，并行度改变了，则状态会在Task中的subtask之间均匀分配
 * 通常用于Source或Sink算子中，用来保存流入数据的偏移量或对输出数据做缓存，以保证Flink应用的Exactly-Once语义
 * OperatorState: 实现CheckpointedFunction，在initializeState方法中初始化状态；Kafka Connector就是使用了OperatorState
 * 1. ListState
 * （1）在系统重启后，ListState快照数据分配采用轮询模式，每个subtask平均分配list数据
 * （2）若getUnionListState()，则快照数据分配采用广播模式，每个subtask持有一份完整的list数据
 * 2. BroadcastState(keyed/non-keyed)
 * (1) it has a map format.
 * (2) an operator can have multiple broadcast states with different names.
 * (3) Broadcast state is kept in-memory at runtime.
 */
public class E07_OperatorState {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        DataStreamSource<String> source1 = env.socketTextStream(UsualConsts.NC_HOST, 9998);
        DataStreamSource<String> source2 = env.socketTextStream(UsualConsts.NC_HOST, 9999);

        doListStateOfOperatorStateSink(source1);
        // doBroadcastWithMapState(source1, source2);
        // doBroadcastWithBroadcastState(source1, source2);

        env.execute(E07_OperatorState.class.getSimpleName());
    }


    private static void doListStateOfOperatorStateSource(StreamExecutionEnvironment env) {
        /**
         * operatorState只能通过initializeState方法获取状态
         */
        class CounterSource extends RichParallelSourceFunction<Long> implements CheckpointedFunction {

            /**
             * current offset for exactly once semantics
             */
            private Long offset = 0L;

            /**
             * flag for job cancellation
             */
            private volatile boolean isRunning = true;

            private final ListStateDescriptor<Long> listStateDescriptor =
                    new ListStateDescriptor<>("counter", LongSerializer.INSTANCE);

            /**
             *  Our state object
             */
            private transient ListState<Long> listState;

            /**
             * checkpoint时调用
             */
            @Override
            public void snapshotState(FunctionSnapshotContext context) throws Exception {
                listState.clear();
                listState.add(offset);
            }

            /**
             * 在任务第一次启动或者重启时调用
             * @param context This is used to initialize the non-keyed state “containers”
             */
            @Override
            public void initializeState(FunctionInitializationContext context) throws Exception {
                listState = context.getOperatorStateStore().getListState(listStateDescriptor);

                for (Long l : listState.get()) {
                    offset = l;
                }
            }

            @Override
            public void run(SourceContext<Long> ctx) throws Exception {
                while (isRunning) {
                    // output and state update are atomic
                    synchronized (ctx.getCheckpointLock()) {
                        ctx.collect(offset);
                        offset += 1;
                    }
                }
            }

            @Override
            public void cancel() {
                isRunning = false;
            }
        }

        env
                .addSource(new CounterSource())
                .print();
    }

    private static void doListStateOfOperatorStateSink(DataStreamSource<String> source) {

        class BufferingSink implements SinkFunction<Tuple2<String, Integer>>, CheckpointedFunction {

            private final int threshold;

            private transient ListState<Tuple2<String, Integer>> checkpointState;

            private final List<Tuple2<String, Integer>> bufferedElements;

            public BufferingSink(int threshold) {
                this.threshold = threshold;
                this.bufferedElements = new ArrayList<>();
            }

            @Override
            public void invoke(Tuple2<String, Integer> value, Context contex) throws Exception {
                // 先将上游数据保存到本地缓存
                bufferedElements.add(value);
                // 当本地缓存大小达到阈值时，将本地缓存输出到外部系统
                if (bufferedElements.size() >= threshold) {
                    for (Tuple2<String, Integer> element : bufferedElements) {
                        // send it to the sink
                    }
                    // 清空本地缓存
                    bufferedElements.clear();
                }
            }

            @Override
            public void snapshotState(FunctionSnapshotContext context) throws Exception {
                checkpointState.clear();
                // 将最新的数据写到状态中
                for (Tuple2<String, Integer> element : bufferedElements) {
                    checkpointState.add(element);
                }
            }

            @Override
            public void initializeState(FunctionInitializationContext context) throws Exception {
                ListStateDescriptor<Tuple2<String, Integer>> descriptor =
                        new ListStateDescriptor<>(
                                "buffered-elements",
                                TypeInformation.of(new TypeHint<Tuple2<String, Integer>>() {
                                }));

                checkpointState = context.getOperatorStateStore().getListState(descriptor);

                // 如果是作业重启，将状态中的数据填充到本地缓存中
                if (context.isRestored()) {
                    for (Tuple2<String, Integer> element : checkpointState.get()) {
                        bufferedElements.add(element);
                    }
                }
            }
        }

        class MyState
                extends RichSinkFunction<Tuple2<String, Integer>>
                implements CheckpointedFunction {

            private transient ListState<Integer> listState;
            private final PrintSinkOutputWriter<Tuple2<String, Integer>> writer = new PrintSinkOutputWriter<>(false);

            @Override
            public void open(Configuration parameters) throws Exception {
                writer.open(getRuntimeContext().getIndexOfThisSubtask(), getRuntimeContext().getNumberOfParallelSubtasks());
            }

            @Override
            public void invoke(Tuple2<String, Integer> value, Context context) throws Exception {
                List<Integer> integers = (List<Integer>) listState.get();
                if (integers.size() == 0) {
                    integers = new ArrayList<>();
                } else {
                    Integer currentValue = integers.get(0);
                    value.f1 += currentValue;
                }
                integers.add(0, value.f1);
                listState.update(integers);
                writer.write(value);
            }

            @Override
            public void snapshotState(FunctionSnapshotContext context) throws Exception {
            }

            @Override
            public void initializeState(FunctionInitializationContext context) throws Exception {
                ListStateDescriptor<Integer> listStateDescriptor = new ListStateDescriptor<>(
                        "list state descriptor",
                        Types.INT);
                listState = context.getOperatorStateStore().getListState(listStateDescriptor);
            }
        }

        source
                .map(
                        line -> {
                            String[] split = line.split("\\s+");
                            return Tuple2.of(split[0], Integer.parseInt(split[1]));
                        }, Types.TUPLE(Types.STRING, Types.INT))
                .addSink(new MyState());
    }

    /**
     * 事实数据
     * 1 图书
     * 2 手机
     * 3 家具
     * 维度数据
     * 1 50
     * 2 2000
     * 最终需要
     * 1 50 图书
     * 2 2000 手机
     * 3 -1 家具
     */
    private static void doBroadcastWithMapState(DataStreamSource<String> factualData,
                                                DataStreamSource<String> dimensionData) {
        // todo define a map state
        MapStateDescriptor<String, Double> mapState =
                new MapStateDescriptor<>("broadcast with map state", String.class, Double.class);

        factualData
                // todo deal with factual data
                .map(
                        line -> {
                            String[] split = line.split("\\s+");
                            return Tuple2.of(split[0], split[1]);
                        }, Types.TUPLE(Types.STRING, Types.STRING))
                .connect(
                        // todo deal with dimension data
                        dimensionData
                                .map(
                                        line -> {
                                            String[] split = line.split("\\s+");
                                            return Tuple2.of(split[0], Double.parseDouble(split[1]));
                                        }, new TupleTypeInfo<>(Types.STRING, Types.DOUBLE))
                                .broadcast(mapState))
                .process(
                        // BroadcastProcessFunction中没有onTimer方法
                        new BroadcastProcessFunction<Tuple2<String, String>, Tuple2<String, Double>,
                                Tuple3<String, Double, String>>() {
                            /**
                             * 为每一条事实数据关联维度
                             *
                             * @param ctx sideOutput、broadcastState、timestamp、processing time、currentWatermark
                             */
                            @Override
                            public void processElement(Tuple2<String, String> value,
                                                       ReadOnlyContext ctx,
                                                       Collector<Tuple3<String, Double, String>> out) {
                                ReadOnlyBroadcastState<String, Double> broadcastState =
                                        ctx.getBroadcastState(mapState);

                                try {
                                    Double price = broadcastState.get(value.f0);
                                    if (Objects.isNull(price)) {
                                        price = -1d;
                                    }
                                    out.collect(Tuple3.of(value.f0, price, value.f1));
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }

                            /**
                             * 当维度数据进入时，更新状态
                             *
                             * @param value The stream element.
                             * @param ctx allows querying the timestamp of the element,
                             *            querying the current processing/event time and updating the broadcast state.
                             *            The context is only valid during the invocation of this method,
                             *            do not store it.
                             * @param out The collector to emit resulting elements to
                             */
                            @Override
                            public void processBroadcastElement(Tuple2<String, Double> value,
                                                                Context ctx,
                                                                Collector<Tuple3<String, Double, String>> out) {
                                BroadcastState<String, Double> broadcastState =
                                        ctx.getBroadcastState(mapState);

                                try {
                                    if (!broadcastState.contains(value.f0)) {
                                        broadcastState.put(value.f0, value.f1);
                                    }
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        })
                .print();
    }

    /**
     * 调用.broadcast()方法，将该DataStream中的数据广播到下游的每个subtask中
     */
    private static void doBroadcastWithBroadcastState(DataStreamSource<String> factualData,
                                                      DataStreamSource<String> dimensionData) {
        class MyCoProcessFunctionWithCheckpointFunction
                extends CoProcessFunction<Tuple2<String, String>, Tuple2<String, Double>, Tuple3<String, Double, String>>
                implements CheckpointedFunction {

            private final MapStateDescriptor<String, Double> mapState =
                    new MapStateDescriptor<>("broadcast with map state", String.class, Double.class);

            private transient BroadcastState<String, Double> broadcastState;

            @Override
            public void snapshotState(FunctionSnapshotContext context) throws Exception {
            }

            @Override
            public void initializeState(FunctionInitializationContext context) throws Exception {
                broadcastState = context.getOperatorStateStore().getBroadcastState(mapState);
            }

            /**
             * 事实流
             *
             * @param ctx sideOutput、timerService、timestamp
             */
            @Override
            public void processElement1(Tuple2<String, String> value,
                                        Context ctx,
                                        Collector<Tuple3<String, Double, String>> out) throws Exception {
                Double price = broadcastState.get(value.f0);
                if (Objects.isNull(price)) {
                    price = -1d;
                }
                out.collect(Tuple3.of(value.f0, price, value.f1));
            }

            /**
             * 维度流
             *
             * @param ctx A {@link Context} that allows querying the timestamp of the element, querying the
             *     {@link TimeDomain} of the firing timer and getting a {@link TimerService} for registering
             *     timers and querying the time. The context is only valid during the invocation of this
             *     method, do not store it.
             */
            @Override
            public void processElement2(Tuple2<String, Double> value,
                                        Context ctx,
                                        Collector<Tuple3<String, Double, String>> out) throws Exception {
                if (!broadcastState.contains(value.f0)) {
                    broadcastState.put(value.f0, value.f1);
                }
            }
        }

        factualData
                // todo deal with factual data
                .map(
                        line -> {
                            String[] split = line.split("\\s+");
                            return Tuple2.of(split[0], split[1]);
                        }, Types.TUPLE(Types.STRING, Types.STRING))
                .connect(
                        // todo deal with dimension data
                        dimensionData
                                .map(
                                        line -> {
                                            String[] split = line.split("\\s+");
                                            return Tuple2.of(split[0], Double.parseDouble(split[1]));
                                        }, new TupleTypeInfo<>(Types.STRING, Types.DOUBLE))
                                .broadcast())
                .process(new MyCoProcessFunctionWithCheckpointFunction()).setParallelism(3)
                .print();
    }

}
