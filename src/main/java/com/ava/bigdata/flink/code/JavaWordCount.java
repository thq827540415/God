package com.ava.bigdata.flink.code;

import com.ava.util.FlinkEnvUtils;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

import java.util.Arrays;

/**
 * @Author lancer
 * @Date 2023/3/6 09:12
 * @Description
 */
public class JavaWordCount {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        env.readTextFile("hdfs://bigdata01:9000/input/word.txt")


        // env.socketTextStream("bigdata01", 9999)
                .flatMap(
                        (String line, Collector<Tuple2<String, Integer>> out) ->
                                Arrays.stream(line.split("\\s+"))
                                        .forEach(word -> out.collect(Tuple2.of(word, 1))), Types.TUPLE(Types.STRING, Types.INT))
                // .returns(Types.TUPLE(Types.STRING, Types.INT))
                .keyBy(t -> t.f0)
                .sum(1)
                .print();

        env.execute();
    }
}
