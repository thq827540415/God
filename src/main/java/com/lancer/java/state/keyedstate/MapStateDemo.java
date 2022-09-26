package com.lancer.java.state.keyedstate;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.HashMap;
import java.util.Map;

public class MapStateDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        env.enableCheckpointing(10000);

        // 辽宁省,大连市,1000
        // 河北省,廊坊市,2000
        // 河北省,廊坊市,2000
        // 辽宁省,沈阳市,1000
        DataStreamSource<String> source = env.socketTextStream("bigdata01", 9999);

        source
                .filter(value -> !"".equals(value.trim()))
                .map(new MapFunction<String, Tuple3<String, String, Double>>() {
                    @Override
                    public Tuple3<String, String, Double> map(String value) throws Exception {
                        String[] words = value.split(",");
                        return Tuple3.of(words[0].trim(), words[1].trim(), Double.parseDouble(words[2].trim()));
                    }
                })
                .keyBy(new KeySelector<Tuple3<String, String, Double>, String>() {
                    @Override
                    public String getKey(Tuple3<String, String, Double> value) throws Exception {
                        return value.f0;
                    }
                })
                // 采用valueState
                .map(new RichMapFunction<Tuple3<String, String, Double>, Tuple3<String, String, Double>>() {

                    private ValueState<Map<String, Double>> cityIncomeState;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        ValueStateDescriptor<Map<String, Double>> valueStateDescriptor = new ValueStateDescriptor<Map<String, Double>>("wc-test", TypeInformation.of(new TypeHint<Map<String, Double>>() {
                        }));
                        cityIncomeState = getRuntimeContext().getState(valueStateDescriptor);
                    }

                    @Override
                    public Tuple3<String, String, Double> map(Tuple3<String, String, Double> value) throws Exception {
                        Map<String, Double> map = cityIncomeState.value();
                        if (map == null) {
                            map = new HashMap<>();
                        }
                        Double history = map.get(value.f1);
                        if (history != null) {
                            value.f2 += history;
                        }
                        map.put(value.f1, value.f2);
                        cityIncomeState.update(map);
                        return value;
                    }
                })
                // 采用mapState
                /*.map(new RichMapFunction<Tuple3<String, String, Double>, Tuple3<String, String, Double>>() {

                    private MapState<String, Double> cityIncomeState;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        MapStateDescriptor<String, Double> mapStateDescriptor = new MapStateDescriptor<>("wc-test", String.class, Double.class);
                        cityIncomeState = getRuntimeContext().getMapState(mapStateDescriptor);
                    }

                    @Override
                    public Tuple3<String, String, Double> map(Tuple3<String, String, Double> value) throws Exception {
                        Double history = cityIncomeState.get(value.f1);
                        if (history != null) {
                            value.f2 += history;
                        }
                        cityIncomeState.put(value.f1, value.f2);
                        return value;
                    }
                })*/
                .print();

        env.execute();
    }
}
