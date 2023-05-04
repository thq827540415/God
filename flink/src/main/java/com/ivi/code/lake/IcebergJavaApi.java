package com.ivi.code.lake;

import lombok.Cleanup;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.hadoop.HadoopCatalog;
import org.apache.iceberg.hive.HiveCatalog;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;
import org.apache.iceberg.types.Types;

import java.io.IOException;
import java.util.Map;

public class IcebergJavaApi {
    /**
     * 操作metadata时，使用iceberg的JavaApi
     * Table interface will return table information.
     */
    private static void createTableByIcebergJavaApi() throws IOException {
        // Catalog properties
        Map<String, String> props =
                ImmutableMap.of(
                        "uri", "thrift://bigdata03:9083",
                        "warehouse", "/user/hive/warehouse",
                        "clients", "2"
                );

        //  todo 1. 通过org.apache.iceberg.hadoop.hadoopCatalog创建表
        @Cleanup HadoopCatalog hadoopCatalog =
                new HadoopCatalog(new Configuration(), "/iceberg/warehouse");

        // todo 2. 通过org.apache.iceberg.hive.HiveCatalog创建表
        HiveCatalog hiveCatalog = new HiveCatalog();
        hiveCatalog.initialize("hive_catalog", props);

        Table table;
        TableIdentifier ti = TableIdentifier.of("db", "flink_iceberg_tbl");

        Schema schema = new Schema(
                Types.NestedField.required(1, "name", Types.StringType.get()),
                Types.NestedField.required(2, "age", Types.IntegerType.get()),
                Types.NestedField.required(3, "dt", Types.StringType.get()));

        PartitionSpec ps = PartitionSpec.builderFor(schema).identity("dt").build();

        if (!hadoopCatalog.tableExists(ti)) {
            table = hadoopCatalog.createTable(ti, schema, ps);
        } else {
            table = hadoopCatalog.loadTable(ti);
        }
    }
}
