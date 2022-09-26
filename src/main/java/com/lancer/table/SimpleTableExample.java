package com.lancer.table;

import com.lancer.FlinkEnvUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import static org.apache.flink.table.api.Expressions.$;
import static org.apache.flink.table.api.Expressions.and;

/**
 * @Author lancer
 * @Date 2022/6/16 23:49
 * @Description Table和SQL的基础案例；DataStream --> Table --> DataStream
 */
public class SimpleTableExample {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment dsEnv = FlinkEnvUtils.getDSEnv();

        SingleOutputStreamOperator<Person> source = dsEnv
                .socketTextStream("localhost", 9999)
                .map(line -> {
                    String[] split = line.split(",");
                    return new Person(split[0], Integer.parseInt(split[1]));
                })
                // POJO的实体类必须是public类型
                .returns(Types.POJO(Person.class));

        // 获取table环境
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .inStreamingMode()
                .useBlinkPlanner()
                .build();
        StreamTableEnvironment tableEnv = FlinkEnvUtils.getTableEnv(dsEnv, settings);

        // 从DataStream创建Table对象
        Table eventTable = tableEnv.fromDataStream(source);

        // 1. 使用Table的方式，DSL语言
        Table resultTable1 = eventTable.select($("name"), $("age"))
                .where($("age").isGreater(20));

        // 2. 使用SQL的方式
        Table resultTable2 = tableEnv.sqlQuery("select name, age from " + eventTable + " where age > 20");

        // 输出结果
        tableEnv.toDataStream(resultTable1)
                .print("tableWay");

        tableEnv.toDataStream(resultTable2)
                .print("sqlWay");

        dsEnv.execute(SimpleTableExample.class.getSimpleName());
    }

    @Data
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Person {
        private String name;
        private int age;
    }
}
