package com.shadow.garden.bigdata.spark.scala.core

import com.shadow.garden.bigdata.util.SparkEnvUtil
import org.apache.spark.SparkConf
import org.apache.spark.broadcast.Broadcast

import scala.collection.immutable.HashMap


object E02_Broadcast {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("broadcast").setMaster("local")
    val sc = SparkEnvUtil.getEnv(conf, enableHiveSupport = false).sparkContext

    val source = sc.textFile("data/stu/input/a.txt")

    val mp = HashMap[Int, String](1 -> "zhangsan", 2 -> "lisi")

    val bc: Broadcast[HashMap[Int, String]] = sc.broadcast(mp)

    val resultRDD = source
      .map(line => {
        val arr = line.split(",")
        (arr(0).toInt, arr(1).toInt, arr(2).toInt, arr(3).toInt, arr(4))
      })
      .mapPartitions(iter => {
        // 广播过来的小表数据
        val smallTable = bc.value

        // 两表进行关联
        iter.map(
          tp => {
            val id = tp._1
            val name = smallTable.getOrElse(id, "未知")
            (id, name, tp._2, tp._3, tp._4, tp._5)
        })
      })

    resultRDD.foreach(println)

    sc.stop()
  }

}
