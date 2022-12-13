package com.ava.bigdata.spark.code.scala.sql

import com.ava.util.SparkEnvUtil
import org.apache.spark.SparkConf
import org.apache.spark.sql.{Encoder, Encoders, Row, SparkSession}
import org.apache.spark.sql.expressions.{Aggregator, MutableAggregationBuffer, UserDefinedAggregateFunction, UserDefinedFunction}
import org.apache.spark.sql.functions.expr
import org.apache.spark.sql.types.{DataType, DoubleType, LongType, StructField, StructType}

/**
 * 弱类型的自定义UDAF函数UserDefinedAggregateFunction将会被废弃，使用强类型替换:Aggregate
 */
object E05_UDAF_CountAverageSalary_DF {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("AverageSalary_DF")
    val spark = SparkEnvUtil.getEnv(conf)

    import spark.implicits._

    val df = spark.sparkContext
      .parallelize(Seq(
        "Michael,3000",
        "Andy,4500",
        "Justin,3500",
        "Berta,4000"
      ))
      .map(line => {
        val arr = line.split(",")
        (arr(0), arr(1).toDouble)
      })
      .toDF("name", "salary")

    /**
     * 计算平均薪资
     */
    // 使用继承UserDefinedAggregateFunction的对象，注册函数，（注册临时表 + SQL）/ DSL语法
    spark.udf.register("avgSal1", udaf = new MyAvgUDAF1)
    // 注册临时表 + SQL
    df.createTempView("df")
    spark.sql(
      """
        |select avgSal1(salary) from df
        |""".stripMargin).show(100, truncate = false)
    // DSL
    df.select(expr("avgSal1(salary) as avgS")).show(100, truncate = false)


    // 使用继承了Aggregator的对象的弱类型，使用SQL
    import org.apache.spark.sql.functions._
    // spark.udf.register("avgSal2", udaf(new MyAvgUDAF2))
    spark.sql(
      """
        |select avgSal2(salary) as avgS from df
        |""".stripMargin).show(100, truncate = false)


    // 使用继承了Aggregator的对象的弱类型，使用DSL
    // val avgSal3: UserDefinedFunction = udaf(new MyAvgUDAF2)
    // df.agg(avgSal3($"salary") as "avgS").show(100, truncate = false)
    // df.select(avgSal3($"salary") as "avgS").show(100, truncate = false)

    df.select()
  }
}


// 进行聚合，就得有缓冲区保存中间结果
class MyAvgUDAF1 extends UserDefinedAggregateFunction {
  // 函数输入的字段schema
  override def inputSchema: StructType = {
    StructType(Seq(
      StructField("salary", DoubleType)
    ))
  }

  // 聚合过程中，用户存储局部聚合结果的schema
  // 比如求平均薪资、中间缓存（局部数据薪资总和，局部数据人数总和）
  override def bufferSchema: StructType = {
    StructType(Array(
      StructField("sum", DoubleType),
      StructField("cnts", LongType)
    ))
  }

  // 函数最终返回结果的最终类型
  override def dataType: DataType = DoubleType

  // 该函数是否是稳定一致的：对相同的一组输入，输出结果永远相等
  override def deterministic: Boolean = true

  // 对局部聚合缓存的初始化方法
  override def initialize(buffer: MutableAggregationBuffer): Unit = {
    buffer.update(0, 0.0) // 根据缓冲区的schema确定buffer字段的个数
    buffer.update(1, 0L)
  }

  // 聚合逻辑，不断传入Row，来更新聚合缓存数据
  override def update(buffer: MutableAggregationBuffer, input: Row): Unit = {
    // 从输入中获取那个人的薪资，加到buffer的第一个字段上
    buffer.update(0, buffer.getDouble(0) + input.getDouble(0))

    // 给buffer的第二个字段->人数加一
    buffer.update(1, buffer.getLong(1) + 1)
  }

  // 全局聚合：将多个局部缓存中的数据，聚合成一个缓存。实质上是一个buffer1和其他Executor上的多个缓冲区buffer2的合并
  override def merge(buffer1: MutableAggregationBuffer, buffer2: Row): Unit = {
    buffer1.update(0, buffer1.getDouble(0) + buffer2.getDouble(0))

    buffer1.update(1, buffer1.getLong(1) + buffer2.getLong(1))
  }

  // 最终输出
  // 从全局缓存中去薪资总和/人数总和
  override def evaluate(buffer: Row): Any = {
    if (buffer.getLong(1) != 0) {
      buffer.getDouble(0) / buffer.getLong(1)
    } else {
      0.0
    }
  }
}


case class Avg(var sum: Double, var count: Long)

class MyAvgUDAF2 extends Aggregator[Long, Avg, Double] {
  override def zero: Avg = Avg(0.0, 0L)

  // 局部聚合
  override def reduce(b: Avg, a: Long): Avg = {
    b.sum += a
    b.count += 1
    b
  }

  // 全局聚合
  override def merge(b1: Avg, b2: Avg): Avg = {
    b1.sum += b2.sum
    b1.count += b2.count
    b1
  }

  override def finish(reduction: Avg): Double = reduction.sum / reduction.count

  /**
   * Encoders.product -> tuple和case class之间的编码器
   */
  override def bufferEncoder: Encoder[Avg] = Encoders.product

  override def outputEncoder: Encoder[Double] = Encoders.scalaDouble
}