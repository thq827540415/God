package com.lancer.flink.table;

import com.google.common.collect.Lists;
import com.lancer.FlinkEnvUtils;
import com.lancer.consts.RedisConsts;
import com.lancer.consts.UsualConsts;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.*;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.connectors.redis.RedisSink;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Schema;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.util.Collector;
import org.apache.hadoop.yarn.webapp.hamlet.Hamlet;

import java.util.*;

import static org.apache.flink.table.api.Expressions.$;

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

        /*env
                .socketTextStream("localhost", 9999)
                .broadcast()
                .map(new Demo())// .setParallelism(1)
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
        // env.execute();

        env
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

        env.execute();
    }
}
