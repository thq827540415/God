package com.lancer.java.state.keyedstate;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.HashSet;
import java.util.Set;

/**
 * 使用状态统计各种活动、点击事件的人数和次数
 * u001,view,活动1
 * u002,view,活动1
 * u001,view,活动1
 * u001,join,活动1
 * u002,join,活动1
 * u001,view,活动2
 * u001,view,活动2
 * u002,view,活动2
 * u002,view,活动2
 * u002,join,活动2
 */
public class Demo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        env.enableCheckpointing(10000);

        env
                .socketTextStream("bigdata01", 9999)
                .filter(line -> !"".equals(line.trim()))
                .map(new MapFunction<String, Tuple3<String, String, String>>() {
                    @Override
                    public Tuple3<String, String, String> map(String value) throws Exception {
                        String[] splits = value.split(",");
                        return Tuple3.of(splits[0].trim(), splits[1].trim(), splits[2].trim());
                    }
                })
                .keyBy(new KeySelector<Tuple3<String, String, String>, Tuple2<String, String>>() {
                    @Override
                    public Tuple2<String, String> getKey(Tuple3<String, String, String> value) throws Exception {
                        return Tuple2.of(value.f1, value.f2);
                    }
                })
                .map(new ActivityFunction())
                .print();

        env.execute();
    }

    private static class ActivityFunction extends RichMapFunction<Tuple3<String, String, String>, Tuple4<String, String, Integer, Integer>> {

        private transient ValueState<Set<String>> valueState; // 后期可以通过采用布隆过滤器代替Set<>
        private transient ValueState<Integer> countState;

        @Override
        public void open(Configuration parameters) throws Exception {
            ValueStateDescriptor<Set<String>> valueStateDescriptor = new ValueStateDescriptor<>("uid-test", TypeInformation.of(new TypeHint<Set<String>>() {
            }));
            ValueStateDescriptor<Integer> countStateDescriptor = new ValueStateDescriptor<>("count-test", Integer.class);
            valueState = getRuntimeContext().getState(valueStateDescriptor);
            countState = getRuntimeContext().getState(countStateDescriptor);
        }

        @Override
        public Tuple4<String, String, Integer, Integer> map(Tuple3<String, String, String> value) throws Exception {
            String uid = value.f0;
            Set<String> uids = valueState.value();
            Integer count = countState.value();
            if (uids == null) {
                uids = new HashSet<>();
            }
            if (count == null) {
                count = 0;
            }
            uids.add(uid);
            count += 1;
            valueState.update(uids);
            countState.update(count);
            return Tuple4.of(value.f2, value.f1, uids.size(), count);
        }
    }
}
