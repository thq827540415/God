package com.ava.bigdata.spark.code.scala.core

import com.ava.util.SparkEnvUtil
import lombok.Cleanup
import org.apache.spark.{SparkConf, SparkFiles}
import org.apache.spark.broadcast.Broadcast

import scala.collection.immutable.HashMap


object E02_Broadcast {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("broadcast").setMaster("local")
    @Cleanup val sc = SparkEnvUtil.getEnv(conf, enableHiveSupport = false).sparkContext

    val source = sc.textFile("data/stu/input/a.txt")

    val mp = HashMap[Int, String](1 -> "zs", 2 -> "ls")

    val bc: Broadcast[HashMap[Int, String]] = sc.broadcast(mp)

    val resultRDD = source
      .map(line => {
        val arr = line.split(",")
        (arr(0).toInt, arr(1).toInt, arr(2).toInt, arr(3).toInt, arr(4))
      })
      .mapPartitions(iter => {
        // 广播过来的小表数据
        val smallTable = bc.value

        // 获取sc.addFile("path")发送的缓存文件
        // SparkFiles.get("path")


        // 两表进行关联
        iter.map(
          tp => {
            val id = tp._1
            val name = smallTable.getOrElse(id, "未知")
            (id, name, tp._2, tp._3, tp._4, tp._5)
        })
      })

    resultRDD.foreach(println)
  }
}
