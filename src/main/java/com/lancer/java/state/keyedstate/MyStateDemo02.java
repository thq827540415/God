package com.lancer.java.state.keyedstate;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义实现类似checkpoint
 */
public class MyStateDemo02 {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        // env.enableCheckpointing(30000);
        // 设置重启策略
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, 2000));
        env.setParallelism(2);

        env
                .socketTextStream("bigdata01", 9999)
                .filter(value -> !"".equals(value))
                .flatMap(new FlatMapFunction<String, String>() {
                    @Override
                    public void flatMap(String value, Collector<String> out) throws Exception {
                        String[] words = value.split(" ");
                        for (String word : words) {
                            out.collect(word);
                        }
                    }
                })
                .map(value -> {
                    if ("error".equals(value)) {
                        throw new RuntimeException("test error");
                    }
                    return Tuple2.of(value, 1);
                }, TypeInformation.of(new TypeHint<Tuple2<String, Integer>>() {
                }))
                .keyBy(value -> value.f0)

                .map(new RichMapFunction<Tuple2<String, Integer>, Tuple2<String, Integer>>() {

                    private Map<String, Integer> counter;
                    private boolean running = true;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        String path = "./Flink-DataStream/mychk/" + getRuntimeContext().getIndexOfThisSubtask() + ".txt";
                        File file = new File(path);
                        if (file.exists()) {
                            // 从文件中读取数据，目的是恢复历史数据
                            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                            counter  = (Map<String, Integer>) ois.readObject();
                            ois.close();
                        } else {
                            file.createNewFile();
                            counter = new HashMap<>();
                        }
                        // 启动一个定时器线程
                        new Thread(() -> {
                            while (running) {
                                try {
                                    // 实际上这个定时器是在JobManager中，然后通过通信，调用TaskManger的subtask中的流方法，保存在别的地方
                                    // 这里是每10s，subtask自动保存其状态信息
                                    Thread.sleep(10000);
                                    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path));
                                    oos.writeObject(counter);
                                    oos.flush();
                                    oos.close();
                                } catch (InterruptedException | IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }

                    @Override
                    public Tuple2<String, Integer> map(Tuple2<String, Integer> value) throws Exception {

                        // 获取当前输入的单词
                        String word = value.f0;
                        // 获取当前输入的次数
                        Integer current = value.f1;
                        Integer history = counter.getOrDefault(word, 0);
                        Integer total = history + current;
                        counter.put(word, total);
                        value.f1 = total;
                        return value;
                    }

                    // 确保程序退出后，定时器线程停止
                    @Override
                    public void close() throws Exception {
                        running = false;
                    }
                })
                .print();

        env.execute();
    }
}
