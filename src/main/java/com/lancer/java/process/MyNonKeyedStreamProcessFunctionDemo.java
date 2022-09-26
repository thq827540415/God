package com.lancer.java.process;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;

public class MyNonKeyedStreamProcessFunctionDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        // 将数据数据中的偶数输出
        // 1 2 3 abc def
        env
                .socketTextStream("bigdata01", 9999)
                .process(new ProcessFunction<String, Integer>() {
                    // 不是最底层的那个processElement
                    @Override
                    public void processElement(String value, Context ctx, Collector<Integer> out) throws Exception {
                        String[] words = value.split(" ");
                        for (String word : words) {
                            try {
                                int num = Integer.parseInt(word);
                                if (num % 2 == 0) {
                                    out.collect(num);
                                }
                            } catch (NumberFormatException e) {
                                // e.printStackTrace();
                            }
                        }
                    }
                })
                .print();

        env.execute();
    }
}
