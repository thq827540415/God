package com.lancer.java.state.keyedstate;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.List;

/**
 * 将用户的最近5次行为保存起来
 */
public class ListStateDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        // env.enableCheckpointing(10000);

        // uid100,view
        // uid102,addCart
        // uid100,view
        // uid100,pay
        DataStreamSource<String> source = env.socketTextStream("bigdata01", 9999);

        source
                .filter(value -> !"".equals(value.trim()))
                .map(new MapFunction<String, Tuple2<String, String>>() {
                    @Override
                    public Tuple2<String, String> map(String value) throws Exception {
                        String[] words = value.split(",");
                        if ("back".equals(words[1].trim())) {
                            throw new RuntimeException("test error");
                        }
                        return Tuple2.of(words[0].trim(), words[1].trim());
                    }
                })
                .keyBy(new KeySelector<Tuple2<String, String>, String>() {
                    @Override
                    public String getKey(Tuple2<String, String> value) throws Exception {
                        return value.f0;
                    }
                })
                // 采用valueState
                .map(new RichMapFunction<Tuple2<String, String>, Tuple2<String, List<String>>>() {

                    // 使用transient关键字，表示该变量不参与序列化，不能通过反序列化赋值
                    // 在这里只能通过open方法赋值
                    private transient ListState<String> listState;

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        ListStateDescriptor<String> listStateDescriptor = new ListStateDescriptor<>("wc-test", String.class);
                        listState = getRuntimeContext().getListState(listStateDescriptor);
                    }

                    @Override
                    public Tuple2<String, List<String>> map(Tuple2<String, String> value) throws Exception {
                        String uid = value.f0;
                        String event = value.f1;
                        /* 方法一 */
                        listState.add(event); // 不用判断是否为空，因为add方法底层会帮每一个key都new ArrayList<>();
                        return Tuple2.of(uid, (List<String>) listState.get());

                        /* 方法二 */
                        // 上面可能会导致list过多
                        /*listState.add(event);
                        List<String> list = (List<String>) listState.get();
                        if (list.size() > 5) {
                            list.remove(0);
                        }
                        // 因为是引用类型，是在第一次add的时候，底层使用了new ArrayList<>()方法,并将该新建的list放入namespace对应的StateTable中，所以不用update，当你自己new一个List的时候，需要update -> 因为数据都在外部的list中，使用update方法，放入内部的list
                        // update方法，底层是将传入的List放入update方法中的newStateList中（List<V> newStateList = new ArrayList<>();），然后再将update中的newStateList放入namespace对应的StateTable中
                        // listState.update(list);
                        return Tuple2.of(uid, list);*/

                        /* 方法三 */
                        /*Iterable<String> events = listState.get();
                        if (!events.iterator().hasNext()) {
                            // ArrayList默认的长度为10
                            events = new ArrayList<>(5);
                        }
                        List<String> list = (List<String>) events;
                        if (list.size() == 5) {
                            list.remove(0);
                        }
                        list.add(event);
                        listState.update(list);
                        return Tuple2.of(uid, list);*/

                        /* 方法四 */
                        /*List<String> list = (List<String>) listState.get();
                        if (list.size() == 0) {
                            list = new ArrayList<>(5);
                        } else if (list.size() == 5) {
                            list.remove(0);
                        }
                        list.add(event);
                        // 数据还未放在stateTable中，故需要update
                        listState.update(list);
                        return Tuple2.of(uid, list);*/
                    }
                })
                .print();

        env.execute();
    }
}
