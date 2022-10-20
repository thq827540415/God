package com.lancer.flink.stream;

import com.lancer.consts.UsualConsts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.ExecutionMode;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichReduceFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.hashmap.HashMapStateBackend;
import org.apache.flink.runtime.state.storage.JobManagerCheckpointStorage;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

@Slf4j
public class StreamingWordCount {
    public static void main(String[] args) throws Exception {
        // StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        Configuration config = new Configuration();
        config.setInteger("rest.bind-port", 8888);
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);


        // todo 执行环境配置
        ExecutionConfig executionConfig = env.getConfig();
        // 默认使用PIPELINED处理模式
        executionConfig.setExecutionMode(ExecutionMode.PIPELINED);


        // todo 取消chain
        // env.disableOperatorChaining();


        // todo 配置checkpoint
        // 1. 开启checkpoint，CheckpointingMode.EXACTLY_ONCE --> 默认
        env.enableCheckpointing(Time.seconds(2).toMilliseconds(), CheckpointingMode.EXACTLY_ONCE);
        // env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        // env.getCheckpointConfig().setCheckpointInterval(2000);

        // 2. cancel job后，是否删除外部的chk-x文件夹 --> 默认删除
        // DELETE_ON_CANCELLATION只有在job is cancelled才会删除
        // kill job == job is failed
        // NO_EXTERNALIZED_CHECKPOINTS暂时未知。。。
        env.getCheckpointConfig().setExternalizedCheckpointCleanup(CheckpointConfig.ExternalizedCheckpointCleanup.DELETE_ON_CANCELLATION);


        // todo 开始checkpoint后，设置重启策略
        // 1. 若没有设置重启策略，则任务失败后，不重启 --> 默认
        env.setRestartStrategy(RestartStrategies.noRestart());

        // 2. 固定次数重启，任务失败后，2s后重启一次，共能重启3次
        // env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, Time.seconds(2)));

        // 3. 当程序失败，5s后重启一次，从失败开始的60s内共能重启3次，60s这个时间内，没达到指定次数，那么重新计数，否则程序退出
        // env.setRestartStrategy(RestartStrategies.failureRateRestart(3, Time.seconds(60), Time.seconds(5)));


        // todo 设置状态后端
        // 1. 使用MemoryStateBackend == HashMapStateBackend + JobManagerCheckpointStorage --> 默认
        env.setStateBackend(new HashMapStateBackend());
        env.getCheckpointConfig().setCheckpointStorage(new JobManagerCheckpointStorage());

        // 2. 使用FsStateBackend == HashMapStateBackend + FileSystemCheckpointStorage
        // 当指定了checkpoint dir后，Flink自动默认使用FileSystemCheckpointStorage
        // env.setStateBackend(new HashMapStateBackend());
        // env.getCheckpointConfig().setCheckpointStorage(UsualConsts.HDFS_URL + "/flink/checkpoint");

        // 3. 使用RocksDBStateBackend == EmbeddedRocksDBStateBackend + FileSystemCheckpointStorage
        // env.setStateBackend(new EmbeddedRocksDBStateBackend());
        // env.getCheckpointConfig().setCheckpointStorage(UsualConsts.HDFS_URL + "/flink/checkpoint");


        // todo Source
        DataStreamSource<String> source = env.socketTextStream(UsualConsts.NC_HOST, 9999);


        // todo Transformation
        SingleOutputStreamOperator<Tuple2<String, Integer>> result = source
                .flatMap(new FlatMapFunction<String, String>() {
                    @Override
                    public void flatMap(String s, Collector<String> collector) throws Exception {
                        String[] words = s.split("\\s+");
                        for (String word : words) {
                            collector.collect(word.toLowerCase().trim());
                        }
                    }
                })
                .filter(new FilterFunction<String>() {
                    @Override
                    public boolean filter(String s) throws Exception {
                        if ("error".equals(s)) {
                            // test restart strategy
                            throw new RuntimeException();
                        }
                        return StringUtils.isNotEmpty(s);
                    }
                }).startNewChain() // 将该算子前面的chain断开
                .map(new MapFunction<String, Tuple2<String, Integer>>() {
                    @Override
                    public Tuple2<String, Integer> map(String word) throws Exception {
                        return new Tuple2<>(word, 1);
                    }
                })
                .keyBy(new KeySelector<Tuple2<String, Integer>, String>() {
                    @Override
                    public String getKey(Tuple2<String, Integer> stringIntegerTuple2) throws Exception {
                        return stringIntegerTuple2.f0;
                    }
                })
                .sum(1).disableChaining(); // 将该算子后面的chain断开


        // todo sink
        result.print();

        env.execute("WordCount");
    }
}
