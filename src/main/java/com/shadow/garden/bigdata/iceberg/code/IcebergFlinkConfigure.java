package com.shadow.garden.bigdata.iceberg.code;

import com.shadow.garden.bigdata.consts.UsualConsts;
import com.shadow.garden.bigdata.util.FlinkEnvUtils;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.Table;
import org.apache.iceberg.actions.RewriteDataFilesActionResult;
import org.apache.iceberg.flink.TableLoader;
import org.apache.iceberg.flink.actions.Actions;
import org.apache.iceberg.flink.sink.FlinkSink;
import org.apache.iceberg.flink.source.FlinkSource;
import org.apache.iceberg.flink.source.IcebergSource;
import org.apache.iceberg.flink.source.StreamingStartingStrategy;
import org.apache.iceberg.flink.source.assigner.SimpleSplitAssignerFactory;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;

import java.time.Duration;
import java.util.Map;

/**
 *
 */
public class IcebergFlinkConfigure {
    public static void main(String[] args) throws Exception {
        // 获取DataStream环境
        org.apache.flink.configuration.Configuration conf = new org.apache.flink.configuration.Configuration();
        // Enable this switch because streaming read SQL will provide few job options in flink SQL hint options.
        conf.setBoolean("table.dynamic-table-options.enabled", true);
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv(conf);

        // 获取table环境
        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .inStreamingMode()
                .build();
        StreamTableEnvironment tableEnv = FlinkEnvUtils.getTableEnv(env, settings);


        Configuration hadoopConf = new Configuration();
        TableLoader tableLoader = TableLoader.fromHadoopTable("/iceberg/warehouse", hadoopConf);

        readFromTableApi(tableEnv);
    }

    /**
     * Iceberg provides API to rewrite small files into large files by submitting flink batch job.
     * The behavior of this flink action is the same as the spark’s rewriteDataFiles.
     */
    private static void rewriteFilesAction(TableLoader tableLoader) {
        Table table = tableLoader.loadTable();
        RewriteDataFilesActionResult result = Actions.forTable(table)
                .rewriteDataFiles()
                .execute();

    }

    //////////////////////////////////////////// Table ////////////////////////////////////////////

    /**
     * 使用SQL创建IcebergCatalog
     */
    private static void createTableByIcebergCatalog(StreamTableEnvironment tableEnv) {
        // 读取core-site.xml
        tableEnv.executeSql(
                "create catalog hadoop_catalog\n" +
                        "with (\n" +
                        "   'type' = 'iceberg',\n" +
                        "   'catalog-type' = 'hadoop',\n" +
                        "   'warehouse' = '/iceberg/warehouse',\n" +
                        "   'property-version' = '1'\n" +
                        ")");

        // 自动读取classpath中的hive-site.xml
        // 或者使用hive-conf-dir指定配置文件目录
        tableEnv.executeSql(
                "create catalog hive_catalog\n" +
                        "with (\n" +
                        "   'type' = 'iceberg',\n" +
                        "   'catalog-type' = 'hive',\n" +
                        "   'uri' = 'thrift://bigdata03:9083',\n" +
                        "   'warehouse' = 'hdfs://bigdata01:9000/user/hive/warehouse',\n" +
                        "   'property-version' = '1',\n" +
                        "   'clients' = '5'\n" +
                        ")");

        tableEnv.useCatalog("hive_catalog1");
        tableEnv.useDatabase("default");
    }

    /**
     * 使用SQL中的with ('connector' = 'iceberg')创建iceberg表
     */
    private static void createTableByConnector(StreamTableEnvironment tableEnv) {
        // Flink's GenericInMemoryCatalog中的表名为default_catalog.default_database.flink_table1
        // Iceberg's HiveCatalog中的表名为hive_catalog.default.iceberg_table
        tableEnv.executeSql(
                "create table flink_table1 (\n" +
                        "   name string,\n" +
                        "   age int,\n" +
                        "   dt string\n" +
                        ") \n" +
                        "partitioned by (dt) \n" +
                        "with (\n" +
                        "   'connector' = 'iceberg',\n" +
                        "   'catalog-type' = 'hive',\n" +
                        "   'catalog-name' = 'hive_catalog',\n" +
                        "   'catalog-database' = 'default',\n" +
                        "   'catalog-table' = 'iceberg_table',\n" +
                        "   'uri' = 'thrift://bigdata03:9083',\n" +
                        "   'warehouse' = '/user/hive/warehouse'\n" +
                        ")");

        // Flink's GenericInMemoryCatalog中的表名为default_catalog.default_database.flink_table2
        // Iceberg's HadoopCatalog中的表名为hive_catalog.default.iceberg_table
        tableEnv.executeSql(
                "create table flink_table2 (\n" +
                        "   name string,\n" +
                        "   age int\n" +
                        ") with (\n" +
                        "   'connector'='iceberg',\n" +
                        "   'catalog-type'='hadoop',\n" +
                        "   'catalog-name'='hadoop_catalog',\n" +
                        "   'catalog-database' = 'default',\n" +
                        "   'catalog-table' = 'iceberg_table',\n" +
                        "   'warehouse'='/iceberg/warehouse'\n" +
                        ")");

        tableEnv.executeSql(
                "insert into flink_table1 values ('zs', 18), ('ls', 20)");

        // Table res = tableEnv.sqlQuery("select * from flink_table1");
    }

    private static void ddl() {
        // Iceberg only support altering table properties in flink 1.11 now.
        String sql = "ALTER TABLE `hive_catalog`.`default`.`flink_table1` SET ('write.format.default'='avro')";
    }


    private static void readFromTableApi(StreamTableEnvironment tableEnv) {
        tableEnv.executeSql(
                "create table flink_table1 (\n" +
                        "   name string,\n" +
                        "   age int,\n" +
                        "   dt string\n" +
                        ") \n" +
                        "partitioned by (dt) \n" +
                        "with (\n" +
                        "   'connector' = 'iceberg',\n" +
                        "   'catalog-type' = 'hive',\n" +
                        "   'catalog-name' = 'hive_catalog',\n" +
                        "   'catalog-database' = 'default',\n" +
                        "   'catalog-table' = 'table_flink1',\n" +
                        "   'uri' = 'thrift://bigdata03:9083',\n" +
                        "   'warehouse' = '/user/hive/warehouse'\n" +
                        ")");

        // batch read
        tableEnv.executeSql("select * from flink_table1")
                .print();

        // streaming read
        tableEnv.executeSql(
                        "select * from flink_table1 /*+ options('streaming'='true', 'monitor-interval'='1s')*/")
                .print();
    }

    //////////////////////////////////////////// DataStream ////////////////////////////////////////////
    private static void readingWithDataStream(
            StreamExecutionEnvironment env, TableLoader tableLoader) throws Exception {

        // 默认Reader参数 ScanContext中的配置
        Map<String, String> props =
                ImmutableMap.of(
                        // enumType
                        "starting-strategy", "StreamingStartingStrategy.INCREMENTAL_FROM_LATEST_SNAPSHOT",
                        // longType
                        "start-snapshot-timestamp", "null",
                        // longType
                        "start-snapshot-id", "null",
                        // longType
                        "end-snapshot-id", "null",
                        // longType
                        "split-size", "null",
                        // intType
                        "split-lookback", "null",
                        // longType
                        "split-file-open-cost", "null",
                        // booleanType
                        "streaming", "false",
                        // durationType
                        "monitor-interval", "Duration.ofSeconds(10L)",
                        // booleanType
                        "include-column-stats", "false"
                );


        // 使用SourceFunction
        FlinkSource.forRowData()
                .env(env)
                .tableLoader(tableLoader)
                .streaming(true)
                .build()
                .print();


        //  Source aims to solve several shortcomings of the old SourceFunction streaming source interface.
        env
                .fromSource(
                        IcebergSource.forRowData()
                                .tableLoader(tableLoader)
                                .assignerFactory(new SimpleSplitAssignerFactory())
                                .streaming(true)
                                .streamingStartingStrategy(StreamingStartingStrategy.INCREMENTAL_FROM_LATEST_SNAPSHOT)
                                .monitorInterval(Duration.ofSeconds(60))
                                .build(),
                        WatermarkStrategy.noWatermarks(),
                        "My Iceberg Source",
                        TypeInformation.of(RowData.class))
                .print();

        env.execute(IcebergFlinkConfigure.class.getSimpleName());
    }

    /**
     * Iceberg support writing to iceberg table from different DataStream input.
     * <p>
     * we have supported writing DataStream<RowData> and DataStream<Row> to the sink iceberg table natively.
     */
    private static void writingWithDataStream(
            StreamExecutionEnvironment env, TableLoader tableLoader) throws Exception {
        DataStream<RowData> input =
                env
                        .socketTextStream(UsualConsts.NC_HOST, 9999)
                        // 3个字段, name、age、dt
                        .map(
                                line -> {
                                    String[] split = line.split(",");
                                    GenericRowData data = new GenericRowData(3);
                                    data.setField(0, split[0]);
                                    data.setField(1, Integer.parseInt(split[1]));
                                    data.setField(2, split[2]);
                                    return data;
                                });

        // 默认Writer参数
        Map<String, String> props =
                ImmutableMap.of(
                        // 对应connector中的write.format.default
                        "write-format", FileFormat.PARQUET.name(),
                        // write.target-file-size-bytes
                        "target-file-size-bytes", "536870912L",
                        // write.upsert.enabled
                        "upsert-enabled", "false",
                        //
                        "overwrite-enabled", "false",
                        // write.distribution-mode
                        "distribution-mode", "none"
                );

        FlinkSink.forRowData(input)
                .tableLoader(tableLoader)
                .setAll(props)
                // OVERWRITE and UPSERT can’t be set together.
                // .upsert(true)
                // .overwrite(true)
                .append();
        env.execute(IcebergFlinkConfigure.class.getSimpleName());
    }
}
