package com.shadow.garden.bigdata.flink.code.stream.transformation;

import com.shadow.garden.util.FlinkEnvUtils;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @Author lancer
 * @Date 2022/6/8 21:39
 * @Description max和maxBy的区别。同理min和minBy的区别
 */
public class E02_MaxAndMaxByOpera {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        env.setParallelism(1);

        DataStreamSource<Tuple3<Long, String, Double>> source = env.fromElements(
                Tuple3.of(100L, "a.com", 20.1),
                Tuple3.of(100L, "b.com", 10.1),
                Tuple3.of(100L, "c.com", 30.1));

        source
                .keyBy(value -> value.f0)
                // max 会替换指定字段的最大值，但是其余字段不会更新，都是旧值，然后输出
                // maxBy会返回最大值所在的那一条信息
                // 都是获取最大值，max不更新其他元素，将指定字段的值更新后，连同旧数据一起返回。maxBy直接将最大值的那条数据返回
                .maxBy(2)
                .print();

        env.execute(E02_MaxAndMaxByOpera.class.getSimpleName());
    }
}
