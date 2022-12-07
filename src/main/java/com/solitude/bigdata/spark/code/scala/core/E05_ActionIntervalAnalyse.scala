package com.solitude.bigdata.spark.code.scala.core

import com.solitude.util.SparkEnvUtil
import org.apache.commons.lang3.time.DateUtils
import org.apache.spark.SparkConf

import scala.collection.mutable.ListBuffer


object E05_ActionIntervalAnalyse {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("actionInterval")
    val sc = SparkEnvUtil.getEnv(conf, enableHiveSupport = false).sparkContext

    // 使用sql语句实现
    // MySQL中没有nvl函数，使用if替代
    // if (unix_timestamp(start_time) - unix_timestamp(if (pre_end_time is null, start_time, pre_end_time)) < 10 * 60, 0, 1) as flag
    val sql =
      """
        |with tmp as (
        | select
        |   uid,
        |   start_time,
        |   end_time,
        |   num,
        |   lag(end_time, 1, null) over(partition by uid order by start_time) as pre_end_time
        | from inInterval
        |)
        |
        |select
        |   uid,
        |   min(start_time) as start_time,
        |   max(end_time) as end_time,
        |   sum(num) as amount
        |from (
        | select
        |   uid,
        |   start_time,
        |   end_time,
        |   num,
        |   sum(flag) over(partition by uid order by start_time) as groupId
        | from (
        |   select
        |     uid,
        |     start_time,
        |     end_time,
        |     num,
        |     if(unix_timestamp(start_time) - nvl(unix_timestamp(pre_end_time), unix_timestamp) < 10 * 60, 0, 1) as flag
        |   from tmp
        | )
        |)t2
        |group by groupId, uid
        |""".stripMargin

    val source = sc.parallelize(Seq(
      "1,2020-02-18 14:20:30,2020-02-18 14:46:30,20",
      "1,2020-02-18 14:47:20,2020-02-18 15:20:30,30",
      "1,2020-02-18 15:37:23,2020-02-18 16:05:26,40",
      "1,2020-02-18 16:06:27,2020-02-18 17:20:49,50",
      "1,2020-02-18 17:21:50,2020-02-18 18:03:27,60",
      "2,2020-02-18 14:18:24,2020-02-18 15:01:40,20",
      "2,2020-02-18 15:20:49,2020-02-18 15:30:24,30",
      "2,2020-02-18 16:01:23,2020-02-18 16:40:32,40",
      "2,2020-02-18 16:44:56,2020-02-18 17:40:52,50",
      "3,2020-02-18 14:39:58,2020-02-18 15:35:53,20",
      "3,2020-02-18 15:36:39,2020-02-18 15:24:54,30"
    ))

    // 判断时间间隔
    def inInterval(dt1: String, dt2: String) = {
      val d1 = DateUtils.parseDate(dt1, "yyyy-MM-dd HH:mm:ss")
      val d2 = DateUtils.parseDate(dt2, "yyyy-MM-dd HH:mm:ss")
      d2.getTime - d1.getTime <= 10 * 60 * 1000 // 10分钟
    }

    source
      .map(line => {
        val arr = line.split(",")
        Action(arr(0), arr(1), arr(2), arr(3).toInt)
      })
      // 每个用户分成一组
      .groupBy(_.uid)
      .flatMap(tp => {
        val uid = tp._1
        //
        val actions: List[Action] = tp._2.toList.sortBy(_.startTime)

        val segments = ListBuffer[List[Action]]()
        var segment = ListBuffer[Action]()

        for (i <- actions.indices) {
          segment += actions(i) // 将该行为放入一段中
          if (!(i != actions.size - 1 && inInterval(actions(i).endTime, actions(i + 1).startTime))) {
            // 当不连续或为最后一个元素时，直接截断
            segments += segment.toList
            segment = ListBuffer[Action]()
          }
        }

        segments
          .map(lst => {
            (uid, lst.head.startTime, lst.reverse.head.endTime, lst.map(_.num).sum)
          })
      })
      .foreach(println)

    sc.stop()
  }
}

/**
 * 用户行为间隔分析
 */
case class Action(uid: String, startTime: String, endTime: String, num: Int)