package com.shadow.garden.bigdata.spark.code.scala.core

import com.shadow.garden.util.{JsonUtils, SparkEnvUtil}
import org.apache.spark.SparkConf
import org.apache.spark.util.AccumulatorV2

import scala.collection.mutable

/**
 * 统计解析出错的JSON行数
 */
object E03_MyAccumulator {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("Accumulator")
    val sc = SparkEnvUtil.getEnv(conf, enableHiveSupport = false).sparkContext

    val source = sc.parallelize(Seq(
      "{\"id\": 1, \"name\": \"lisi\", \"age\": 12}",
      "{\"id\": \"a\", \"name\": \"zs\", \"age\": 11}",
      "{\"id\": 3, \"name\": \"ww\"}"
    ))

    val myAccumulator = new CustomAccumulator(new mutable.HashMap[String, Long])

    // 注册全局累加器
    sc.register(myAccumulator)

    val res = source.map(json => {
      try {
        val node = JsonUtils.parseObject(json)
        val id = node.get("id").asInt()
        val name = node.get("name").asText()
        val age = node.get("age").asInt()
        (id, name, age)
      } catch {
        case _: NullPointerException => myAccumulator.add(("nullPointerException", 1))
        case _: NumberFormatException => myAccumulator.add(("numberFormatException", 1))
        case _ => myAccumulator.add(("other", 1))
          null
      }
    })

    res.foreach(println)

    println("=============================")

    println(myAccumulator.value)

    sc.stop()
  }
}

class CustomAccumulator(map: mutable.HashMap[String, Long]) extends AccumulatorV2[(String, Long), mutable.HashMap[String, Long]] {
  override def isZero: Boolean = map.isEmpty

  override def copy(): AccumulatorV2[(String, Long), mutable.HashMap[String, Long]] = {
    val hashMap = new mutable.HashMap[String, Long]()
    this.map.foreach(kv => hashMap.put(kv._1, kv._2))
    new CustomAccumulator(hashMap)
  }

  override def reset(): Unit = this.map.clear()

  override def add(v: (String, Long)): Unit = {
    /*val key: String = v._1
    val oldV = this.map.getOrElse(key, 0)
    val newV: Long = v._2
    this.map.put(key, oldV + newV)*/
  }

  override def merge(other: AccumulatorV2[(String, Long), mutable.HashMap[String, Long]]): Unit = {
    other.value.foreach(kv => {
      /*val key: String = kv._1
      val value: Long = this.map.getOrElse(key, 0) + kv._2
      this.map.put(key, value)*/
    })
  }

  override def value: mutable.HashMap[String, Long] = this.map
}