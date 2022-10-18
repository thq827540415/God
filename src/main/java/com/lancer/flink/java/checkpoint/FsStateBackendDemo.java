package com.lancer.flink.java.checkpoint;

import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class FsStateBackendDemo {

    static {
        System.setProperty("HADOOP_USER_NAME", "root");
    }

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());


        env.enableCheckpointing(Time.seconds(30).toMilliseconds());
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, 2000));
        env.setStateBackend(new FsStateBackend("hdfs://bigdata01:9000/flink_chk")); // 新版本存入HDFS，通过修改集群配置文件

        env
                .socketTextStream("bigdata01", 9999)
                .filter(value -> !"".equals(value))
                .map(value -> {
                    if ("error".equals(value)) {
                        throw new RuntimeException("test error");
                    }
                    return Tuple2.of(value, 1);
                }, TypeInformation.of(new TypeHint<Tuple2<String, Integer>>() {
                }))
                .keyBy(value -> value.f0)
                .sum(1)
                .print();

        env.execute();
    }
}
