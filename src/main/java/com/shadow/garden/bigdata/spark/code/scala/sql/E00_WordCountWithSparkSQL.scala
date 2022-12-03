package com.shadow.garden.bigdata.spark.code.scala.sql

import com.shadow.garden.bigdata.util.SparkEnvUtil
import org.apache.spark.SparkConf

object E00_WordCountWithSparkSQL {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
      .setAppName("wordCount")
      .set("spark.sql.shuffle.partitions", "2")
      .set("spark.default.parallelism", "1")
    val spark = SparkEnvUtil.getEnv(conf, enableHiveSupport = false)

    val df = spark.read.text("file:///D:\\IdeaProjects\\Spark\\data\\word.txt")

    df.createTempView("df")

    spark.sql(
      """
        |select word,
        |       count(1) as num
        |from (
        |   select explode(split(value, '\\s+')) as word
        |   from df
        |) t
        |group by word
        |order by num desc
        |
        |""".stripMargin).show(100, truncate = false)

    // DSL
    import spark.implicits._
    import org.apache.spark.sql.functions._
    df.select(explode(split($"value", "\\s+")) as "word")
      .groupBy("word")
      .agg(count($"word") as "num")
      .orderBy($"num".desc)
      .show(100, truncate = false)

    spark.close()
  }
}
