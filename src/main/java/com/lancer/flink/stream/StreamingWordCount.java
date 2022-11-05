package com.lancer.flink.stream;

import com.lancer.util.FlinkEnvUtils;
import com.lancer.consts.UsualConsts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.ExecutionMode;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.runtime.state.storage.JobManagerCheckpointStorage;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.time.Duration;
import java.util.Arrays;

@Slf4j
public class StreamingWordCount {
    public static void main(String[] args) throws Exception {
        // todo 获取执行环境
        Configuration conf = new Configuration();
        conf.setInteger("rest.bind-port", 8888);
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv(conf);


        // todo 执行环境配置
        ExecutionConfig executionConfig = env.getConfig();
        // 默认使用PIPELINED处理模式
        executionConfig.setExecutionMode(ExecutionMode.PIPELINED);


        // todo 取消operator chain
        // env.disableOperatorChaining();


        // todo 配置checkpoint
        // 1. 开启checkpoint，指定两次checkpoint的间隔时间，CheckpointingMode.EXACTLY_ONCE --> 默认
        // 指定checkpoint的对齐方式，默认为对齐；AT_LEAST_ONCE为非对齐
        env.enableCheckpointing(2000, CheckpointingMode.EXACTLY_ONCE);
        // env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        // env.getCheckpointConfig().setCheckpointInterval(Time.seconds(2).toMilliseconds());

        // 2. 允许checkpoint失败最大的次数
        env.getCheckpointConfig().setTolerableCheckpointFailureNumber(0);

        // 3. 设置checkpoint对齐的超时时间
        env.getCheckpointConfig().setAlignedCheckpointTimeout(Duration.ofMillis(500));

        // 4. 如果检查点5s还没创建完毕，则认为该次checkpoint失败
        env.getCheckpointConfig().setCheckpointTimeout(5000);

        // 5. cancel job后，是否删除外部的chk-x文件夹 --> 默认删除
        // DELETE_ON_CANCELLATION只有在job is cancelled才会删除
        // kill job == job is failed
        // NO_EXTERNALIZED_CHECKPOINTS暂时未知。。。
        env.getCheckpointConfig().setExternalizedCheckpointCleanup(
                CheckpointConfig.ExternalizedCheckpointCleanup.DELETE_ON_CANCELLATION);

        // 6. 最大并行的checkpoint数量
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(2);

        // 7. 创建检查点完成后（并不是创建检查点的周期），创建下一个检查点最少需要暂停的时间
        // ck达到最大并行时，若之前的ck任务还未完成，则下一次的ck操作将会排队，占用系统的额外资源，此时就需要合理配置以下参数
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(3000);


        // todo 开始checkpoint后，设置重启策略
        // 1. 若没有设置重启策略，则任务失败后，不重启 --> 默认
        env.setRestartStrategy(RestartStrategies.noRestart());

        // 2. 固定次数重启，任务失败后，2s后重启一次，共能重启3次
        // env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, Time.seconds(2)));

        // 3. 当程序失败，5s后重启一次，从失败开始的60s内共能重启3次，60s这个时间内，没达到指定次数，那么重新计数，否则程序退出
        // env.setRestartStrategy(RestartStrategies.failureRateRestart(3, Time.seconds(60), Time.seconds(5)));


        // todo 设置state backend
        // 1. 使用MemoryStateBackend == HashMapStateBackend + JobManagerCheckpointStorage --> 默认
        // 使用createKeyedStateBackend创建AbstractKeyedStateBackend(HeapKeyedStateBackend)
        // 使用createOperatorStateBackend创建OperatorStateBackend
        // 在new StreamTask的时候创建状态后端
        env.setStateBackend(new HashMapStateBackend());
        env.getCheckpointConfig().setCheckpointStorage(new JobManagerCheckpointStorage());

        // 2. 使用FsStateBackend == HashMapStateBackend + FileSystemCheckpointStorage
        // 当指定了checkpoint dir后，Flink自动默认使用FileSystemCheckpointStorage
        // env.setStateBackend(new HashMapStateBackend());
        // env.getCheckpointConfig().setCheckpointStorage(UsualConsts.HDFS_URL + "/flink/checkpoint");

        // 3. 使用RocksDBStateBackend == EmbeddedRocksDBStateBackend + FileSystemCheckpointStorage
        // HashMapStateBackend和EmbeddedRocksDBStateBackend快照文件的格式是统一的
        // env.setStateBackend(new EmbeddedRocksDBStateBackend());
        // env.getCheckpointConfig().setCheckpointStorage(UsualConsts.HDFS_URL + "/flink/checkpoint");


        // todo watermark
        // 设置Watermark发送的周期
        env.getConfig().setAutoWatermarkInterval(500);


        // todo Source
        DataStreamSource<String> source = env.socketTextStream(UsualConsts.NC_HOST, 9999);


        // todo Transformation
        SingleOutputStreamOperator<Tuple2<String, Integer>> result =
                source
                        .flatMap(
                                (s, collector) -> {
                                    Arrays.stream(s.split("\\s+"))
                                            .forEach(word -> collector.collect(word.toLowerCase().trim()));
                                }, Types.STRING)
                        .filter(
                                s -> {
                                    if ("error".equals(s)) {
                                        // test restart strategy
                                        throw new RuntimeException();
                                    }
                                    return StringUtils.isNotEmpty(s);
                                }).startNewChain() // 将该算子前面的chain断开
                        .map(
                                word -> new Tuple2<>(word, 1),
                                new TypeHint<Tuple2<String, Integer>>() {
                                }.getTypeInfo())
                        // 分配数据的流程如下： maxParallelism == the number of key groups
                        //      KeyGroupStreamPartitioner#selectChannel ->
                        //          KeyGroupRangeAssignment#computeOperatorIndexForKeyGroup
                        //              KeyGroupRangeAssignment#assignToKeyGroup ->
                        // 先计算key所属的key group，再计算对应key group对应的subtask，最终返回subtask index
                        .keyBy(t -> t.f0, TypeInformation.of(String.class))
                        // 当有多个keyBy时，最后一个的keyBy的的KeySelector会替换之前的
                        .keyBy(t -> t.f0, TypeInformation.of(String.class))
                        .sum(1).disableChaining(); // 将该算子后面的chain断开


        // todo sink
        result
                .map(value -> value, Types.TUPLE(Types.STRING, Types.INT))
                // global：上游的数据只分发给下游的第一个分区
                // broadcast：上游的数据广播给下游的每个分区
                // forward：上下游并发度必须一样，数据一对一发送
                // shuffle：随机均匀分配，网络开销大
                // rebalance：轮询发送，网络开销大
                // rescale：TM本地轮询发送，网络开销小
                // keyBy：hash发送，每个key发送到对应的分区 key -> key group -> subtask
                // partitionCustom：custom，每个key发送到对应的KeyGroup
                .forward()
                // 新开一个名为test的slot共享组，只有提交到集群上才有用，否则资源分配不到位，会导致subtask无法启动
                // slot的个数与cores的数量相等最好
                // slot = 每个slot共享组中，最大的并行度之和
                // 若下游算子没有指定slot共享组，则会继承上游的slot共享组，默认的slot共享组名为default
                .print()// .slotSharingGroup(SlotSharingGroup.newBuilder("test").build())
        ;

        env.execute("WordCount");
    }
}
