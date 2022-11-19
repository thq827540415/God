package com.shadow.garden.bigdata.spark.scala.sql

import com.shadow.garden.bigdata.util.SparkEnvUtil
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.expressions.UserDefinedFunction

import scala.collection.mutable

/**
 * 定义函数实现两个向量之间的余弦相似度计算
 */
object E04_UDF_CountCosineSimilarity {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("Similarity").set("spark.default.parallelism", "1").set("spark.sql.shuffle.partitions", "1")
    val spark = SparkEnvUtil.getEnv(conf)

    import spark.implicits._

    val df = spark.sparkContext
      .parallelize(Seq(
        "1,a,18,172,120,98,68.8",
        "2,b,28,175,120,97,68.8",
        "3,c,30,180,130,94,88.8",
        "4,d,18,168,110,98,68.8",
        "5,e,26,165,120,98,68.8",
        "6,f,27,182,135,95,89.8",
        "7,g,19,171,122,99,68.8"
      ))
      .map(line => {
        val arr = line.split(",")
        (arr(0).toInt, arr(1), arr(2).toDouble, arr(3).toDouble, arr(4).toDouble, arr(5).toDouble, arr(6).toDouble)
      })
      .toDF("id", "name", "age", "height", "weight", "face", "score")

    // 将df转成id、name、features的形式
    import org.apache.spark.sql.functions._

    val features = df.select($"id", $"name", array("height", "weight", "face", "score") as "features")

    // 连接自己
    val summaryT = features.join(features.toDF("uId", "uName", "uFeatures"), $"id" < $"uId")

    // 自定义函数 --> 计算余弦相似度 cos ß = ∑(xi * yi) / (sqrt(∑ xi^2) * sqrt(∑ yi^2))       [-1, 1]
    val cosineSim = (f1: mutable.WrappedArray[Double], f2: mutable.WrappedArray[Double]) => {
      val firstDenominator = math.pow(f1.map(math.pow(_, 2)).sum, 0.5)
      val secondDenominator = math.pow(f2.map(math.pow(_, 2)).sum, 0.5)

      val numerator = f1.zip(f2).map(tp => tp._1 * tp._2).sum

      numerator / (firstDenominator * secondDenominator) // 分子 / 分母
    }

    // spark.udf.register("cos_sim", udf(cosineSim))
    spark.udf.register("cos_sim", cosineSim) // sql风格
    /*summaryT.createTempView("t")
    com.lancer.scala.sql(
      """
        |select
        | id,
        | uId,
        | cos_sim(features, uFeatures) as cos_similar
        |from t
        |
        |""".stripMargin).show(100, truncate = false)*/
    // summaryT.select($"id", $"uid", expr("cos_sim(features, uFeatures) as cos_similar")).show(100, truncate = false)

    val cos_sim: UserDefinedFunction = udf(cosineSim) // DSL风格 注册UDF函数
    summaryT.select($"id", $"uid", cos_sim(col("features"), summaryT("uFeatures")) as "cos_similar")
      .show(100, truncate = false)

    spark.close()
  }
}
