package com.lumine.bigdata.spark.scala.structured

import com.lumine.bigdata.util.SparkEnvUtil
import org.apache.spark.SparkConf
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.streaming.OutputMode
import org.apache.spark.sql.streaming.Trigger.ProcessingTime

/**
 * @Author lancer
 * @Date 2022/2/11 10:31 下午
 * @Description
 */
object E01_WordCountWithStructuredStreaming {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("WordCountWithStructuredStreaming")
    val spark = SparkEnvUtil.getEnv(conf)

    import spark.implicits._

    val lines = spark
      .readStream
      // 支持file source、kafka source、socket source
      .format("socket")
      .option("host", "localhost")
      .option("port", 9999)
      .load()

    val words: Dataset[String] = lines.as[String].flatMap(_.split(" ")).as("value")

    val wordCounts = words.groupBy("value").count()


    // 将数据存与内存的表中
    wordCounts
      .writeStream
      .queryName("ad_pv_tb")
      // 有聚合计算使用complete，没有使用append
      .outputMode(OutputMode.Complete())
      // 设置检查点路径
      .option("checkpointLocation", "hdfs://structured_streaming/checkpoint/dir")
      // 支持file sink、foreach sink、console sink、memory sink
      .format("memory")
      .start()

    spark.sql("select * from ad_pv_tb").show()

    // 将数据存入外部存储系统
    wordCounts
      .writeStream
      .trigger(ProcessingTime("10 seconds"))
      .outputMode("complete") // append
      .format("console")
      .start()
      .awaitTermination()
  }
}
