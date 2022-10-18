package com.lancer.flink.stream;

import com.lancer.FlinkEnvUtils;
import lombok.*;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import java.util.Objects;
import java.util.Properties;

/**
 * @Author lancer
 * @Date 2022/6/7 15:35
 * @Description 将年龄 > 18的用户的信息输出到kafka
 */
public class SideOutputStream2Kafka {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        Properties p = new Properties();
        p.load(SideOutputStream2Kafka.class.getClassLoader().getResourceAsStream("producer.properties"));

        // 1. 从socketStream中获取数据源
        DataStreamSource<String> source = env.socketTextStream("localhost", 9999);

        // 2. 定义测流输出标签
        OutputTag<String> outputTag = new OutputTag<String>("adult"){
        };

        SingleOutputStreamOperator<User> outputStream = source
                // 3. 将获取到的数据过滤并解析成User对象 ==> sz,12    ls,25
                .flatMap((FlatMapFunction<String, User>) (line, out) -> {
                    if (Objects.nonNull(line)) {
                        String[] split = line.split(",");
                        int age = Integer.parseInt(split[1]);
                        if (split.length == 2 && age >= 0 && age <= 100) {
                            out.collect(new User(split[0], age));
                        }
                    }
                })
                .returns(TypeInformation.of(User.class))
                // 4. 将18岁以上的放入侧 流中
                .process(new ProcessFunction<User, User>() {
                    @Override
                    public void processElement(User user, ProcessFunction<User, User>.Context ctx, Collector<User> out) {
                        if (user.getAge() >= 18) {
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
                .addSink(new FlinkKafkaProducer<>("test", new SimpleStringSchema(), p));

        /**
         * 另一种写法
         * outputStream
         *                 .getSideOutput(outputTag)
         *                 .addSink(new FlinkKafkaProducer<>(
         *                         "test",
         *                         (element, timestamp) -> new ProducerRecord<>("test", element.toString().getBytes(StandardCharsets.UTF_8)),
         *                         p,
         *                         FlinkKafkaProducer.Semantic.EXACTLY_ONCE));
         */
        env.execute(SideOutputStream2Kafka.class.getSimpleName());
    }


    @Data
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    private static class User {
        private String name;
        private int age;
    }
}
