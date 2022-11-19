package com.shadow.garden.bigdata.spark.scala.sql

import com.shadow.garden.bigdata.consts.MySQLConsts
import com.shadow.garden.bigdata.util.SparkEnvUtil
import org.apache.spark.SparkConf
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.types.{DataTypes, StructField, StructType}
import org.apache.spark.sql.{Dataset, Row, SparkSession}

import java.util.Properties
import scala.collection.immutable.HashMap


object E03_ReadFromJdbc {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("readFromJdbc").setMaster("local")
    val spark = SparkEnvUtil.getEnv(conf)

    val df1 = spark.read.jdbc(MySQLConsts.URL, "inInterval", new Properties())

    /**
     * 关联数据第一种方式 -- 广播变量实现map join
     */
    // 学习中这样使用，平常数据都不是从本地读取, 可以读取数据到driver的RDD或者DF/DS，然后转成本地集合，发送到Executor端
    val mp = HashMap[Int, String](1 -> "scan", 2 -> "shopping", 3 -> "gaming") // 小表数据

    val bc = spark.sparkContext.broadcast(mp) // 将数据广播出去

    /**
     * 返回Row对象，需要传入Row对象的编码器：RowEncoder，其他对象，需要对应的Encoder
     */
    val encoder = RowEncoder( // 指定字段的格式信息，用于将返回的Row进行序列化和反序列化
      StructType(Seq(
        StructField("uid", DataTypes.IntegerType),
        StructField("name", DataTypes.StringType),
        StructField("start_time", DataTypes.StringType),
        StructField("end_time", DataTypes.StringType),
        StructField("num", DataTypes.IntegerType)
      )))

    // 调用RDD算子，是先变成RDD，然后通过隐式转换中的Encoder，变成DF/DS
    val resultDf1: Dataset[Row] = df1.mapPartitions(iter => {
      val smallTable = bc.value

      iter.map(row => { // 使用广播的方式实现map join
        val uid = row.getAs[Int]("uid")
        val start_time = row.getAs[String]("start_time")
        val end_time = row.getAs[String]("end_time")
        val num = row.getAs[Int]("num")
        val name = smallTable.getOrElse(uid, "未知")
        Row(uid, name, start_time, end_time, num)
      })
    })(encoder)

    resultDf1.show(100, truncate = false)

    /**
     * 返回tuple，基本类型都是默认使用隐式转换中的ExpressionEncoder
     */
    import spark.implicits._

    val resultDf2: Dataset[(Int, String, String, String, Int)] = df1.mapPartitions(iter => {
      val smallTable = bc.value

      iter.map(row => { // 使用广播的方式实现map join
        val uid = row.getAs[Int]("uid")
        val start_time = row.getAs[String]("start_time")
        val end_time = row.getAs[String]("end_time")
        val num = row.getAs[Int]("num")
        val name = smallTable.getOrElse(uid, "未知")
        // 由数据类型指定字段类型
        (uid, name, start_time, end_time, num)
      })
    })

    // 指定字段名
    resultDf2.toDF("uid", "name", "start_time", "end_time", "num").show(100, truncate = false)


    /**
     * 关联数据第二种方式 -- 两个df写sql实现join
     */
    val df2 = spark
      .read
      .schema(
        StructType(Seq(
          StructField("id", DataTypes.IntegerType),
          StructField("name", DataTypes.StringType)
        )))
      .csv("Spark-core/data/stu/input/b.txt")

    // 将两个df转成临时视图
    df1.createTempView("df1")
    df2.createTempView("df2")

    spark.sql(
      """
        |select
        | uid,
        | name,
        | start_time,
        | end_time,
        | num
        |from df1 left join df2 on df1.uid = df2.id
        |
        |""".stripMargin).show(100, truncate = false)
    spark.close()
  }
}
