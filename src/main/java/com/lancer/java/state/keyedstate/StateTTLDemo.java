package com.lancer.java.state.keyedstate;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.StateTtlConfig;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.time.Time;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

/**
 * 只有KeyedState可以设置TTL
 */
public class StateTTLDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

       env.enableCheckpointing(10000);

        env
                .socketTextStream("bigdata01", 9999)
                .filter(value -> !"".equals(value.trim()))
                .flatMap(new FlatMapFunction<String, String>() {
                    @Override
                    public void flatMap(String value, Collector<String> out) throws Exception {
                        String[] splits = value.split(" ");
                        for (String word : splits) {
                            out.collect(word);
                        }
                    }
                })
                .map(new MapFunction<String, Tuple2<String, Integer>>() {
                    @Override
                    public Tuple2<String, Integer> map(String value) throws Exception {
                        return Tuple2.of(value, 1);
                    }
                })
                .keyBy(new KeySelector<Tuple2<String, Integer>, String>() {
                    @Override
                    public String getKey(Tuple2<String, Integer> value) throws Exception {
                        return value.f0;
                    }
                })
                .map(new RichMapFunction<Tuple2<String, Integer>, Tuple2<String, Integer>>() {

                    private transient ValueState<Integer> valueState;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        // 设置单个状态的超时时间，通过env设置checkpoint的超时时间
                        StateTtlConfig ttlConfig = StateTtlConfig.newBuilder(Time.seconds(60))
                                // .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite) // 默认存活时间更新类型
                                // .setUpdateType(StateTtlConfig.UpdateType.Disabled) // 关闭TTL，状态永不过期
                                .setUpdateType(StateTtlConfig.UpdateType.OnReadAndWrite)
                                .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired) // 数据只要超时就不能使用了
                                // .setStateVisibility(StateTtlConfig.StateVisibility.ReturnExpiredIfNotCleanedUp) // 数据只要还没被cleanUp（开个线程），就还可以使用
                                .build();
                        ValueStateDescriptor<Integer> valueStateDescriptor = new ValueStateDescriptor<>("wc-test", Integer.class);
                        // 将ttl和descriptor关联到一起
                        valueStateDescriptor.enableTimeToLive(ttlConfig);

                        valueState = getRuntimeContext().getState(valueStateDescriptor);
                    }

                    @Override
                    public Tuple2<String, Integer> map(Tuple2<String, Integer> value) throws Exception {
                        Integer history = valueState.value();
                        if (history != null) {
                            value.f1 += history;
                        }
                        valueState.update(value.f1);
                        return value;
                    }
                })
                .print();

        env.execute();
    }
}
