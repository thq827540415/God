package com.lancer.stream;

import com.lancer.FlinkEnvUtils;
import org.apache.flink.api.common.functions.Partitioner;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @Author lancer
 * @Date 2022/6/16 09:30
 * @Description
 */
public class SelfPartitioner {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        DataStreamSource<String> source = env.socketTextStream("localhost", 9999);

        source.print();

        env.execute(SelfPartitioner.class.getSimpleName());
    }

    private static class MyPartitioner implements Partitioner<String> {
        @Override
        public int partition(String key, int numPartitions) {
            return 0;
        }
    }
}
