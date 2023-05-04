package com.ivi.bigdata.spark.code.scala

import org.apache.spark.{SparkConf, SparkContext}

/**
 * @Author lancer
 * @Date 2023/2/16 10:27
 * @Description
 */
object Demo {
  def main(args: Array[String]): Unit = {
    /*
    val conf = new SparkConf().setAppName("Demo").setMaster("local")
    val spark = SparkEnvUtil.getEnv(conf = conf, enableHiveSupport = false)

    val value = spark.sparkContext.makeRDD(
      Seq("hello world", "hello spark", "hello world", "spark spark"))
      .flatMap(_.split(" ")).mapPartitions()
      .map((_, 1))
      .reduceByKey(_ + _)


    println(value.dependencies)
    val res = value
      .map { case (k, v) => (v, k) }
      .sortByKey(ascending = false)
      // take一个首个partition的数据
      .take(2)
      .toList

    try {
      println("hello")
    }

    println(res)

    spark.stop*/

    def quickSort(list: List[Int]): List[Int] =
      list match {
        case Nil => Nil
        case List() => List()
        case head :: tail =>
          val (left, right) = tail.partition(_ < head)
          quickSort(left) ::: head :: quickSort(right)
      }

    def merge(left: List[Int], right: List[Int]): List[Int] =
      (left, right) match {
        case (Nil, _) => right
        case (_, Nil) => left
        case (x :: xTail, y :: yTail) =>
          if (x <= y) x :: merge(xTail, right)
          else y :: merge(left, yTail)
      }

    println(quickSort(List(3, 2, 34, 5)))

    println(merge(List(1, 3, 5), List(2, 4, 6)))
  }
}
