package com.lancer.flink.table;

import com.google.common.collect.Lists;
import com.lancer.util.FlinkEnvUtils;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.*;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.util.*;

public class E01_Kafka2HBase {
    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .inStreamingMode()
                .build();

        StreamTableEnvironment tableEnv = FlinkEnvUtils.getTableEnv(env, settings);

        tableEnv.executeSql(
                "create temporary table record (" +
                        "line string" +
                        ") with (" +
                        "'connector' = 'kafka'," +
                        "'topic' = 'test'," +
                        "'properties.bootstrap.servers' = 'bigdata01:9092'," +
                        "'properties.group.id' = 'test-group'," +
                        "'scan.startup.mode' = 'latest-offset'," +
                        "'value.format' = 'raw')");

        class Demo extends RichMapFunction<String, String> implements CheckpointedFunction {

            private transient ListState<String> listState;

            @Override
            public String map(String value) throws Exception {
                List<String> list = Lists.newArrayList(listState.get());

                if (list.size() == 0) {
                    list.add(value + "-" + 1);
                } else {
                    boolean flag = true;
                    for (int i = 0; i < list.size(); i++) {
                        String[] split = list.get(i).split("-");
                        if (value.equals(split[0])) {
                            flag = false;
                            list.add(value + "-" + (Integer.parseInt(split[1]) + 1));
                            list.remove(i);
                            break;
                        }
                    }
                    if (flag) {
                        list.add(value + "-" + 1);
                    }
                }

                listState.update(list);
                list.sort(
                        (o1, o2) ->
                                Integer.parseInt(o2.split("-")[1]) - Integer.parseInt(o1.split("-")[1]));
                StringBuilder sb = new StringBuilder();
                int size = Math.min(list.size(), 3);
                for (int i = 0; i < size; i++) {
                    if (i != size - 1) {
                        sb.append(list.get(i)).append(",");
                    } else {
                        sb.append(list.get(i));
                    }
                }
                return sb.toString();
            }

            @Override
            public void snapshotState(FunctionSnapshotContext context) throws Exception {
            }

            @Override
            public void initializeState(FunctionInitializationContext context) throws Exception {
                ListStateDescriptor<String> test =
                        new ListStateDescriptor<>("test", new TypeHint<String>() {
                        }.getTypeInfo());
                listState = context.getOperatorStateStore().getListState(test);
            }
        }

        class OperatorStateByMap extends RichMapFunction<String, String>
            implements CheckpointedFunction {

            private transient ListState<Map<String, Integer>> listState;
            @Override
            public String map(String value) throws Exception {
                List<Map<String, Integer>> list = Lists.newArrayList(listState.get());
                if (list.size() == 0) {
                    list.add(0, new HashMap<>());
                }
                Map<String, Integer> stringIntegerMap = list.get(0);
                stringIntegerMap.put(value, stringIntegerMap.getOrDefault(value, 0) + 1);
                listState.update(list);


                List<Map.Entry<String, Integer>> entries = new ArrayList<>(stringIntegerMap.entrySet());
                entries.sort((o1, o2) -> o2.getValue() - o1.getValue());

                StringBuilder sb = new StringBuilder();
                int size = Math.min(3, entries.size());
                for (int i = 0; i < size; i++) {
                    if (i != size - 1) {
                        sb.append(entries.get(i)).append(",");
                    } else {
                        sb.append(entries.get(i));
                    }
                }
                return sb.toString();
            }

            @Override
            public void snapshotState(FunctionSnapshotContext context) throws Exception {
            }

            @Override
            public void initializeState(FunctionInitializationContext context) throws Exception {
                ListStateDescriptor<Map<String, Integer>> test =
                        new ListStateDescriptor<>("test", new TypeHint<Map<String, Integer>>() {
                        }.getTypeInfo());
                listState = context.getOperatorStateStore().getListState(test);
            }
        }

        env
                .socketTextStream("localhost", 9999)
                // .broadcast()
                .map(new OperatorStateByMap()).setParallelism(1).print();
        /*
                .addSink(
                        new RedisSink<>(
                                new FlinkJedisPoolConfig.Builder()
                                        .setHost(RedisConsts.REDIS_HOST)
                                        .setPort(RedisConsts.REDIS_PORT)
                                        .setDatabase(0)
                                        .build(),
                                new RedisMapper<String>() {
                                    @Override
                                    public RedisCommandDescription getCommandDescription() {
                                        return new RedisCommandDescription(RedisCommand.SET);
                                    }

                                    @Override
                                    public String getKeyFromData(String s) {
                                        return "total";
                                    }

                                    @Override
                                    public String getValueFromData(String s) {
                                        return s;
                                    }
                                }));*/
        env.execute();

        /*env
                .socketTextStream("localhost", 9999)
                .keyBy(line -> 0)
                .process(
                        new KeyedProcessFunction<Integer, String, String>() {

                            private transient MapState<String, Integer> mapState;

                            @Override
                            public void open(Configuration parameters) throws Exception {

                                MapStateDescriptor<String, Integer> mapStateDescriptor = new MapStateDescriptor<>("test", String.class, Integer.class);
                                mapState = getRuntimeContext().getMapState(mapStateDescriptor);
                            }

                            @Override
                            public void processElement(String value, Context ctx, Collector<String> out) throws Exception {

                                Integer integer = mapState.get(value);
                                if (Objects.isNull(integer)) {
                                    mapState.put(value, 1);
                                } else {
                                    mapState.put(value, integer + 1);
                                }

                                List<Map.Entry<String, Integer>> list = new ArrayList<>();
                                for (Map.Entry<String, Integer> entry : mapState.entries()) {
                                    list.add(entry);
                                }
                                list.sort(new Comparator<Map.Entry<String, Integer>>() {
                                    @Override
                                    public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                                        return o2.getValue() - o1.getValue();
                                    }
                                });

                                StringBuilder sb = new StringBuilder();
                                int size = Math.min(list.size(), 3);
                                for (int i = 0; i < size; i++) {
                                    if (i != size -1) {
                                        sb.append(list.get(i)).append(",");
                                    } else {
                                        sb.append(list.get(i));
                                    }
                                }
                                out.collect(sb.toString());
                            }
                        }
                ).setParallelism(1)
                .print();

        env.execute();*/
    }
}
