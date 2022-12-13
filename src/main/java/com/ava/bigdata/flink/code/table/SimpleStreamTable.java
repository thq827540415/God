package com.ava.bigdata.flink.code.table;

import com.ava.consts.CommonConstants;
import com.ava.util.FlinkEnvUtils;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.*;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.hive.HiveCatalog;

import static org.apache.flink.table.api.Expressions.$;

/**
 * @Author lancer
 * @Date 2022/6/16 23:49
 * @Description Table和SQL的基础案例；DataStream --> Table --> DataStream
 */
public class SimpleStreamTable {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment dsEnv = FlinkEnvUtils.getDSEnv();
        dsEnv.setRuntimeMode(RuntimeExecutionMode.STREAMING);

        SingleOutputStreamOperator<Person> source =
                dsEnv
                        .socketTextStream(CommonConstants.NC_HOST, 9999)
                        .map(
                                line -> {
                                    String[] split = line.split(",");
                                    return new Person(split[0], Integer.parseInt(split[1]));
                                })
                        // POJO的实体类必须是public类型
                        .returns(Types.POJO(Person.class));

        // 获取table环境
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .inStreamingMode()
                .build();
        StreamTableEnvironment tableEnv = FlinkEnvUtils.getTableEnv(dsEnv, settings);
        // 通过CatalogManager中的LinkedHashMap<String, Catalog>保存catalog
        // 一个tableEnv中可以有多种类型的catalog，catalogs.put(defaultCatalogName, defaultCatalog);
        // 默认为default_catalog -> GenericInMemoryCatalog
        // 每种类型的catalog中又可以保存多种database和table
        tableEnv.registerCatalog(
                "hive_catalog",
                new HiveCatalog(
                        "hive",
                        "default",
                        null));


        // 在tableEnv中设置默认的catalog和database
        // 若不指定catalog和database，则访问表的默认值 -> default_catalog.default_database.${table}
        // tableEnv.useCatalog("hive_catalog");
        // tableEnv.useDatabase("default");
        // 设置使用Hive的SQL语法
        // tableEnv.getConfig().setSqlDialect(SqlDialect.HIVE);


        // todo 在默认Catalog中注册临时视图和表
        // 1. 注册临时视图
        tableEnv.createTemporaryView("tempView1", source);
        // 2. 创建一张永久表
        // tableEnv.createTable("tempView", TableDescriptor.forConnector("kafka").build());
        // 3. 创建一张临时表
        // tableEnv.createTemporaryTable("tempView", TableDescriptor.forConnector("kafka").build());
        // 5. 通过SQL创建
        // tableEnv.executeSql("create temporary view tempView() with ('connector' = '')");


        // todo Table对象的创建
        // 1. 通过在Catalog中注册的视图或表的名称
        // Table table = tableEnv.from("tempView");
        // 2. 通过表描述器，核心是connector
        // tableEnv.from(TableDescriptor.forConnector("kafka").build());
        // 3. 从DataStream创建Table对象。如果流类型是Row，则使用fromChangelogStream()
        // 会通过反射手段，自动推断schema
        Table eventTable = tableEnv.fromDataStream(source,
                Schema.newBuilder()
                        .column("name", DataTypes.STRING())
                        .column("age", DataTypes.INT())
                        .build());


        // 1. 使用Table API的方式，DSL语言
        Table resultTable1 = eventTable
                .select($("name"), $("age"))
                .where($("age").isGreater(20));

        // 2. 使用SQL的方式
        Table resultTable2 = tableEnv.sqlQuery("select name, age from " + eventTable + " where age > 20");

        // 输出结果
        tableEnv.toDataStream(resultTable1)
                .print("tableWay");

        tableEnv.toDataStream(resultTable2)
                .print("sqlWay");

        dsEnv.execute(SimpleStreamTable.class.getSimpleName());
    }

    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Person {
        public String name;
        public int age;
    }
}
