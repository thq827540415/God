package com.lancer.flink.cdc;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

/**
 * @Author com.com.lancer
 * @Date 2022/4/18 23:01
 * @Description
 */
public class MySqlTableExample {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        StreamTableEnvironment tblEnv = StreamTableEnvironment.create(env);

        tblEnv.executeSql(
                "create table cdc_test_flink (" +
                        "id int," +
                        "name string" +
                        ") with (" +
                        "'connector' = 'mysql-cdc'," +
                        "'hostname' = 'bigdata03'," +
                        "'port' = '3306'," +
                        "'username' = 'root'," +
                        "'password' = '123456'," +
                        "'database-name' = 'project'," +
                        "'table-name' = 'cdc_test'," +
                        "'scan.startup.mode' = 'latest-offset'," +
                        "'scan.incremental.snapshot.enabled' = 'false'" +
                        ")");

        Table table = tblEnv.sqlQuery("select * from cdc_test_flink");
        DataStream<Tuple2<Boolean, Row>> retractStream = tblEnv.toRetractStream(table, Row.class);
        retractStream.print().setParallelism(1);

        env.execute("CDC Table API");
    }
}
