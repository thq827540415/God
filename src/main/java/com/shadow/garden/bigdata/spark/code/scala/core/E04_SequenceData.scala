package com.shadow.garden.bigdata.spark.code.scala.core

import com.solitude.util.SparkEnvUtil
import org.apache.commons.lang3.time.DateUtils
import org.apache.spark.SparkConf

/**
 * 连续日期
 */
object E04_SequenceData {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("SequenceData")
    val sc = SparkEnvUtil.getEnv(conf, enableHiveSupport = false).sparkContext

    // 连续日期的sql实现
    // 在MySQL命令行中，要用date_sub(dt, interval rk day)实现日期减去rk天
    // 在Hive中，使用date_sub(dt, rk)实现减去rk天
    // SparkSQL使用的是HiveSQL的语法
    val sql =
      """
        |with tmp as (
        | select guid, dt, row_number() over(partition by guid order by dt) rk
        | from sequence
        |)
        |
        |select
        | guid,
        | min(dt) as start_dt,
        | max(dt) as end_dt,
        | count(1) as num
        |from t
        |group by date_sub(dt, rk), guid
        |having count(1) >= 3
        |""".stripMargin

    val source = sc.parallelize(Seq(
      "guid01,2018-02-28",
      "guid01,2018-03-01",
      "guid01,2018-03-02",
      "guid01,2018-03-05",
      "guid01,2018-03-04",
      "guid01,2018-03-06",
      "guid01,2018-03-07",
      "guid02,2018-03-01",
      "guid02,2018-03-02",
      "guid02,2018-03-03",
      "guid02,2018-03-06"
    ))

    source
      .map(line => { // 先将数据切分
        val arr = line.split(",")
        (arr(0), arr(1))
      })
      // 相同用户分到一组
      .groupByKey()
      .flatMap(tp => {
        tp._2
          .toList
          // 将日期进行排序
          .sorted
          // 每条数据添加行号
          .zipWithIndex
          .map(t => { // 计算对应的分区号
            (t._1, DateUtils.addDays(DateUtils.parseDate(t._1, "yyyy-MM-dd"), -t._2))
          })
          // 将分区号相同的进行分组，即连续的日期分一组
          .groupBy(_._2)
          .map(g => {
            // 日期的开始时间、结束时间、连续天数
            (tp._1, g._2.head._1, g._2.reverse.head._1, g._2.size)
          })
      })
      // 过滤掉连续日期少于3天的
      .filter(_._4 >= 3)
      .foreach(println)

    sc.stop()
  }
}
