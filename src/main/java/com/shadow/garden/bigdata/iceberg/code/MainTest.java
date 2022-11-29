package com.shadow.garden.bigdata.iceberg.code;

import com.shadow.garden.bigdata.util.FlinkEnvUtils;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.iceberg.hive.HiveCatalog;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;

import java.util.Map;

public class MainTest {
    public static void main(String[] args) {
        // 获取DataStream环境
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        // 获取table环境
        // GenericInMemoryCatalog -> FlinkSQL中默认使用的Catalog
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .inStreamingMode()
                .build();
        StreamTableEnvironment tableEnv = FlinkEnvUtils.getTableEnv(env, settings);


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
