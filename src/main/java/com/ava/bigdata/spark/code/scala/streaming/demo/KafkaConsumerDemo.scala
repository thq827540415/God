package com.ava.bigdata.spark.code.scala.streaming.demo

import org.apache.kafka.clients.consumer.KafkaConsumer

import java.time.Duration
import java.util.Properties

/**
 * @Author lancer
 * @Date 2022/6/1 17:42
 * @Description
 */
object KafkaConsumerDemo {
  def main(args: Array[String]): Unit = {
    val properties = new Properties()
    properties.load(KafkaConsumerDemo.getClass.getClassLoader.getResourceAsStream("consumer.properties"))

    val consumer = new KafkaConsumer(properties)
    import scala.collection.JavaConversions._
    consumer.subscribe(List("demo"))

    while (true) {
      val recorders = consumer.poll(Duration.ofMillis(1000))
      recorders.foreach(record => {
        println(s"topic:${record.topic()}\tpartition:${record.partition()}\toffset:${record.offset()}\tkey:${record.key()}\tvalue:${record.value()}")
        Thread.sleep(1000)
      })
    }
  }
}
