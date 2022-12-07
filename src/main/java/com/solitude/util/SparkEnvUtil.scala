package com.solitude.util

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

/**
 * @Author lancer
 * @Date 2022/2/23 4:49 下午
 * @Description
 */
object SparkEnvUtil {

  System.setProperty("HADOOP_USER_NAME", "root")

  def getEnv(conf: SparkConf = new SparkConf(), enableHiveSupport: Boolean = true): SparkSession = {
    if (enableHiveSupport) {
      SparkSession
        .builder
        .config(conf)
        .config("spark.sql.warehouse.dir", "hdfs://bigdata01:9000/user/hive/warehouse")
        .config("spark.hadoop.hive.metastore.warehouse.dir", "hdfs://bigdata01:9000/user/hive/warehouse")
        .config("spark.hadoop.hive.metastore.uris", "thrift://bigdata03:9083")
        .enableHiveSupport()
        .getOrCreate()
    } else {
      SparkSession
        .builder()
        .config(conf = conf)
        .getOrCreate()
    }
  }
}
