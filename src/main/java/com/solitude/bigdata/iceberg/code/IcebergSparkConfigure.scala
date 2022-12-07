package com.solitude.bigdata.iceberg.code

import org.apache.spark.sql.SparkSession

/**
 * 创建的Iceberg表都是外部表
 */
object IcebergSparkConfigure {

  System.setProperty("HADOOP_USER_NAME", "root")

  def main(args: Array[String]): Unit = {

    val spark = SparkSession.builder()
      .config("spark.sql.warehouse.dir", "hdfs://bigdata01:9000/user/hive/warehouse")
      // Spark内置的Catalog有两种（InMemoryCatalog -> 默认和HiveExternalCatalog）
      // 开启Hive支持，Spark内置的Catalog(spark_catalog)变为HiveExternalCatalog
      .enableHiveSupport()

      // 向Spark中添加新命令如call 、alter，spark2.4不支持
      .config("spark.sql.extensions", "org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions")
      // 在hive中创建iceberg表，否则创建的是普通表，同时hive得有iceberg的依赖，才能在hive中访问iceberg表
      .config("iceberg.engine.hive.enabled", "true")

      // ======================================= SparkCatalog ======================================
      // 创建一个名为hive_prod的catalog，用于从HMS中导入iceberg表
      .config("spark.sql.catalog.hive_prod", "org.apache.iceberg.spark.SparkCatalog")
      .config("spark.sql.catalog.hive_prod.type", "hive")
      // hive.metastore.uris的值会读取hive-site.xml
      .config("spark.sql.catalog.hive_prod.uri", "thrift://bigdata03:9083")
      .config("spark.sql.catalog.hive_prod.default-namespace", "default")

      // 将元数据保存在内存中，操作表在HDFS上
      .config("spark.sql.catalog.hadoop_prod", "org.apache.iceberg.spark.SparkCatalog")
      .config("spark.sql.catalog.hadoop_prod.type", "hadoop")
      .config("spark.sql.catalog.hadoop_prod.warehouse", "hdfs://bigdata01:9000/iceberg/warehouse")
      // 使用spark.sql.catalog.hadoop_prod.hadoop.*代替spark.hadoop.*
      .config("spark.sql.catalog.hadoop_prod.hadoop.fs.defaultFS", "hdfs://bigdata01:9000")
      .config("spark.hadoop.fs.defaultFS", "hdfs://bigdata01:9000")


      // ==================================== SparkSessionCatalog ===================================
      // ⭐SparkSessionCatalog给Spark内置的Catalog（spark_catalog）添加访问iceberg表的支持
      // ⭐即给Spark内置的catalog包装(wrap)一下，让其拥有访问hive中iceberg表的能力
      // ⭐所有的操作将会优先使用包装过的spark_catalog
      // SparkSessionCatalog还存在诸多弊端，推荐使用SparkCatalog。比如访问表的元数据信息时，只能用DataFrame
      .config("spark.sql.catalog.spark_catalog", "org.apache.iceberg.spark.SparkSessionCatalog")
      .config("spark.sql.catalog.spark_catalog.type", "hive")
      .master("local")
      .getOrCreate()


    // 如果配置了spark_catalog = org.apache.iceberg.spark.SparkSessionCatalog
    // 1. 当创建表时：
    //      （1）不指定catalog:
    //          i. 如果不是iceberg表，会采用spark_catalog包装内部的原生的Catalog，此时原生的Catalog需要为HiveExternalCatalog
    //          ii. 如果是iceberg表，则会采用包装后的spark_catalog
    //      （2）指定catalog时，由于配置的Catalog的type都是hive，故能直接访问Hive
    // 2. 当查询不指定catalog时，Spark会去包装后的spark_catalog中找表


    // ==================================== CREATE ===================================
    // Spark2.4没有DDL语句，建表只能通过SQL建
    // 只有原生catalog才能创建普通表，其他的catalog创建的都是iceberg表（外部表）
    // 创建表时，using语法是SparkCatalog、SparkSessionCatalog特有的，Spark内置的Catalog无法解析
    // CTAS和RTAS在SparkCatalog中是具有原子性，而在SparkSessionCatalog中不具有原子性

    // 1.
    /*val frame = spark.sql(
      """
        |create table person(
        |   name string,
        |   age int
        |) using iceberg
        |
        |""".stripMargin)*/


    // ==================================== SELECT ===================================
    // spark.table(table)和spark.read.format("iceberg").load(table)中的table可以一样，底层都是使用iceberg的解析器
    // 可取的值为 -> /path/to/table、catalog.namespace.table、namespace.table、table等

    // 1. 普通表的值查询
    spark.sql("select * from default.person").show(false)

    // 2. 在Spark3.3之前的版本，只能通过DataFrame查询某一时刻的数据：
    //    （1）通过时间戳as-of-timestamp（单位ms）和通过snapshot-id
    //    （2）Spark3.0版本及之前，不支持option(...).table(...)
    // spark.read.option("as-of-timestamp", "1667557570996").table("default.person").show(false)
    // spark.read.format("iceberg").option("as-of-timestamp", "1667557570996").load("hive_prod.test.person")
    // spark.read.format("iceberg").option("snapshot-id", 0L).load("hdfs://bigdata01:9000/user/hive/warehouse/person")

    // 3. 只能通过DataFrame增量查询 [start-snapshot-id, end-snapshot-id)：
    //    （1）如果不指定end-snapshot-id，则end-snapshot-id为当前的snapshot-id
    // spark.read.format("iceberg").option("start-snapshot-id", 0L).option("end-snapshot-id", 0L).load("")
    // spark.read.option("start-snapshot-id", 0L).option("end-snapshot-id", 1L).table("default.person")

    // 4. inspect table -> 查看表元数据时（history、snapshot and other metadata）：
    //    （1）在Spark2.4只支持DataFrame
    //    （2）在Spark3.x，SparkSessionCatalog中，不支持用multipart identifiers，只能通过DataFrame使用，而SparkCatalog什么都支持
    // i. SparkSessionCatalog
    // spark.read.format("iceberg").load("default.person.history")
    // ii. SparkCatalog
    // spark.read.format("iceberg").load("hive_prod.default.test.history")
    // spark.read.table("hive_prod.default.test.history")
    // spark.sql("select * from hive_prod.default.test.history")


    // ==================================== WRITE ===================================
    // Spark2.4只支持DataFrame的Append和Overwrite
    spark.close()
  }
}
