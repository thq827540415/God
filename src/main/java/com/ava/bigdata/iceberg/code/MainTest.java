package com.ava.bigdata.iceberg.code;

import com.ava.util.FlinkEnvUtils;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

public class MainTest {
    public static void main(String[] args) {
        // 获取DataStream环境

        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .inStreamingMode()
                .build();

        TableEnvironment tableEnv = FlinkEnvUtils.getTableEnv(settings);


        tableEnv.executeSql(
                "create catalog hive_catalog\n" +
                        "with (\n" +
                        "    'type' = 'iceberg',\n" +
                        "    'catalog-type' = 'hive',\n" +
                        "    'property-version' = '1',\n" +
                        "    'clients' = '5'\n" +
                        ")"
        );

        tableEnv.executeSql(
                "create table hive_catalog.database_default.flink_table1 (" +
                        "name string," +
                        "age int)");
    }
}
