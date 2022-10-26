package com.lancer.flink.table;

import com.google.common.collect.Lists;
import com.lancer.FlinkEnvUtils;
import com.lancer.consts.UsualConsts;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.util.Collector;

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

            private transient ListState<HashMap<String, Void>> listState;

            @Override
            public String map(String value) throws Exception {
                List<HashMap<String, Void>> list = Lists.newArrayList(listState.get());
                HashMap<String, Void> map = list.get(0);
                if (Objects.isNull(map)) {
                    map = new HashMap<>();
                }
                return null;
            }

            @Override
            public void snapshotState(FunctionSnapshotContext context) throws Exception {

            }

            @Override
            public void initializeState(FunctionInitializationContext context) throws Exception {
                ListStateDescriptor<HashMap<String, Void>> test =
                        new ListStateDescriptor<>("test", new TypeHint<HashMap<String, Void>>(){}.getTypeInfo());
                listState = context.getOperatorStateStore().getListState(test);
            }
        }

        env
                .socketTextStream(UsualConsts.NC_HOST, 9999)
                .map(new Demo())
                .print();

        env.execute();
    }
}
