package com.ava.bigdata.flink.code.cdc;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

/**
 * @Author shadow
 * @Date 2022/4/18 23:01
 * @Description
 */
public class MySqlTable {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        StreamTableEnvironment tblEnv = StreamTableEnvironment.create(env);

        tblEnv.executeSql(
                "create table cdc_test_flink (\n" +
                        "   id int,\n" +
                        "   name string\n" +
                        ") with (\n" +
                        "   'connector' = 'mysql-cdc',\n" +
                        "   'hostname' = 'bigdata03',\n" +
                        "   'port' = '3306',\n" +
                        "   'username' = 'root',\n" +
                        "   'password' = '123456',\n" +
                        "   'database-name' = 'project',\n" +
                        "   'table-name' = 'cdc_test',\n" +
                        "   'scan.startup.mode' = 'latest-offset',\n" +
                        "   'scan.incremental.snapshot.enabled' = 'false'" +
                        ")");

        Table table = tblEnv.sqlQuery("select * from cdc_test_flink");
        DataStream<Tuple2<Boolean, Row>> retractStream = tblEnv.toRetractStream(table, Row.class);
        retractStream.print().setParallelism(1);

        env.execute("CDC Table API");
    }
}
