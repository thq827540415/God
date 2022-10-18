package com.lancer.flink.java.partitioner;

import com.lancer.TransformationApp;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.Partitioner;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.ParallelSourceFunction;

import java.util.Random;

public class PartitionerApp {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        test01(env);

        env.execute("PartitionerApp");
    }

    public static void test01(StreamExecutionEnvironment env) {

        env.setParallelism(3); // 设置全局并行度

        // 按字符串做相应分区
        class PKPartitioner implements Partitioner<String> {
            // 方法返回值，就是分区号
            @Override
            public int partition(String key, int d) {
                if ("com.com.lancer.com".equals(key)) {
                    return 0;
                } else if ("a.com".equals(key)) {
                    return 1;
                } else {
                    return 2;
                }
            }
        }

        class AccessSource implements ParallelSourceFunction<TransformationApp.Access> {
            private boolean running = true;

            @Override
            public void run(SourceContext<TransformationApp.Access> ctx) throws Exception {
                String[] domains = new String[]{"com.com.lancer.com", "a.com", "b.com"};
                Random r = new Random();
                while (running) {
                    for (int i = 0; i < 10; i++) {
                        ctx.collect(new TransformationApp.Access(1234567L, domains[r.nextInt(domains.length)], r.nextDouble() + 1000));
                    }
                    Thread.sleep(10000);
                }
            }

            @Override
            public void cancel() {
                running = false;
            }
        }

        // Source
        DataStreamSource<TransformationApp.Access> source = env.addSource(new AccessSource());
        System.out.println("source's parallelism：" + source.getParallelism());

        // Transform
        SingleOutputStreamOperator<TransformationApp.Access> sourceStream = source
                .map(new MapFunction<TransformationApp.Access, Tuple2<String, TransformationApp.Access>>() {
                    @Override
                    public Tuple2<String, TransformationApp.Access> map(TransformationApp.Access value) throws Exception {
                        return Tuple2.of(value.getDomain(), value);
                    }
                })
                // selectChannel中调用partition方法
                .partitionCustom(new PKPartitioner(), new KeySelector<Tuple2<String, TransformationApp.Access>, String>() {
                    @Override
                    public String getKey(Tuple2<String, TransformationApp.Access> value) throws Exception {
                        return value.f0;
                    }
                })
                .map(new MapFunction<Tuple2<String, TransformationApp.Access>, TransformationApp.Access>() {
                    @Override
                    public TransformationApp.Access map(Tuple2<String, TransformationApp.Access> value) throws Exception {
                        System.out.println("current thread id is: " + Thread.currentThread().getId() + ", value is: " + value.f1);
                        return value.f1;
                    }
                });

        // Sink
        sourceStream.print();
    }
}
