package com.lancer.bigdata.spark.scala.core

import com.lancer.bigdata.util.SparkEnvUtil
import org.apache.spark.SparkConf

object E00_WordCountWithScala {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("WC")
    val sc = SparkEnvUtil.getEnv(conf, enableHiveSupport = false).sparkContext

    // 存在闭包引用
    var count = 0

    sc.textFile("input.txt")
      .filter(_.nonEmpty)
      .flatMap(line => line.split("\\s+"))
      .map((_, 1))
      .reduceByKey(_ + _)
      .foreach(x => {
        count += 1
        println(count, x)
      })
    sc.stop()
  }
}
