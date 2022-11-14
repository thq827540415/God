package com.lancer.bigdata.hudi;

import com.lancer.bigdata.util.FlinkEnvUtils;
import com.lancer.bigdata.util.FlinkEnvUtils;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

public class HoodieFirst {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();


        // 专门用于RocksDS
        // PredefinedOptions.SPINNING_DISK_OPTIMIZED_HIGH_MEM

        StreamTableEnvironment tableEnv = FlinkEnvUtils.getTableEnv(env);


        tableEnv.executeSql(
                "create table sourceT(" +
                        "uuid varchar(20), \n" +
                        "name varchar(10), \n" +
                        "age int, \n" +
                        "ts timestamp(3), \n" +
                        "`partition` varchar(20) \n" +
                        ") with (" +
                        "'connector' = 'datagen', \n" +
                        "'rows-per-second' = '1'\n" +
                        ")");

        env.execute(HoodieFirst.class.getSimpleName());
    }
}
