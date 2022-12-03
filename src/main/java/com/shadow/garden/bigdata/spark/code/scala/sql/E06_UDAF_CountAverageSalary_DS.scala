package com.shadow.garden.bigdata.spark.code.scala.sql

import com.shadow.garden.bigdata.util.SparkEnvUtil
import org.apache.spark.SparkConf
import org.apache.spark.sql.{Encoder, Encoders, SparkSession, TypedColumn}
import org.apache.spark.sql.expressions.Aggregator


/**
 * 强类型自定义UDAF函数，求平均薪资
 */
object E06_UDAF_CountAverageSalary_DS {

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("AverageSalary_DS")
    val spark = SparkEnvUtil.getEnv(conf)

    import spark.implicits._

    val ds = spark.sparkContext
      .parallelize(Seq(
        "Michael,3000",
        "Andy,4500",
        "Justin,3500",
        "Berta,4000"
      ))
      .map(line => {
        val arr = line.split(",")
        Employee(arr(0), arr(1).toDouble)
      })
      .toDS()

    // 使用单例对象创建聚合函数, Dataset使用这种方式自定义函数；DataFrame通过注册函数，再通过SQL或者DSL语法使用
    val avgSal: TypedColumn[Employee, Double] = (new MyAggregate).toColumn.name("avgSal")
    // DSL
    ds.select(avgSal).show(100, truncate = false)

    spark.close()
  }
}

case class Employee(name: String, salary: Double)
case class Average(var sum: Double, var count: Long)

class MyAggregate extends Aggregator[Employee, Avg, Double] {
  // 定义一个初始值，保存工资总数和工资总个数 --> 手动定义的case class，初始值都为0
  override def zero: Avg = Avg(0.0, 0L)

  // 一个Executor上，合并两个值，生成一个新值
  override def reduce(b: Avg, a: Employee): Avg = {
    b.sum += a.salary
    b.count += 1
    b
  }

  // 聚合不同Executor的结果
  override def merge(b1: Avg, b2: Avg): Avg = {
    b1.sum += b2.sum
    b1.count += b2.count
    b1
  }

  // 计算输出结果
  override def finish(reduction: Avg): Double = reduction.sum / reduction.count

  // 设定之间值类型的编码器，要转换成case类
  // Encoders.product是进行scala元组和case类转换的编码器
  override def bufferEncoder: Encoder[Avg] = Encoders.product

  // 设定最终输出值的编码器
  override def outputEncoder: Encoder[Double] = Encoders.scalaDouble
}