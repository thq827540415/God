package com.shadow.garden.bigdata.flink.code.stream.transformation;

import com.shadow.garden.consts.Consts;
import com.shadow.garden.util.FlinkEnvUtils;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.util.Objects;

/**
 * @Author lancer
 * @Date 2022/6/7 15:35
 * @Description 将年龄 > 18的用户的信息输出到kafka
 */
public class E03_SideOutputStream {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        // 1. 从socketStream中获取数据源
        DataStreamSource<String> source = env.socketTextStream(Consts.NC_HOST, 9999);

        // 2. 定义测流输出标签
        OutputTag<String> outputTag = new OutputTag<String>("adult") {
        };

        SingleOutputStreamOperator<Tuple2<String, Integer>> outputStream = source
                // 3. 将获取到的数据过滤并解析成User对象 ==> sz,12    ls,25
                .flatMap(
                        (FlatMapFunction<String, Tuple2<String, Integer>>) (line, out) -> {
                            if (Objects.nonNull(line)) {
                                String[] split = line.split(",");
                                int age = Integer.parseInt(split[1]);
                                if (split.length == 2 && age >= 0 && age <= 100) {
                                    out.collect(Tuple2.of(split[0], age));
                                }
                            }
                        }, Types.TUPLE(Types.STRING, Types.INT))
                // 4. 将18岁以上的放入侧流中
                .process(
                        new ProcessFunction<Tuple2<String, Integer>, Tuple2<String, Integer>>() {
                            @Override
                            public void processElement(
                                    Tuple2<String, Integer> user,
                                    ProcessFunction<Tuple2<String, Integer>, Tuple2<String, Integer>>
                                            .Context ctx,
                                    Collector<Tuple2<String, Integer>> out) {
                                if (user.f1 >= 18) {
                                    ctx.output(outputTag, user.toString());
                                } else {
                                    out.collect(user);
                                }
                            }
                        });

        // 5. 输出不满18的用户的信息
        outputStream.print();

        // 6. 将测流中的数据放入kafka中
        outputStream
                .getSideOutput(outputTag)
                .print("adult");

        env.execute(E03_SideOutputStream.class.getSimpleName());
    }

}
