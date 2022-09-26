package com.lancer.scala.source

import com.lancer.scala.transformations.Access
import org.apache.flink.api.common.functions.MapFunction
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.source.{RichParallelSourceFunction, RichSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer
import org.apache.flink.util.{Collector, NumberSequenceIterator}

import java.util.Properties
import scala.util.Random


object SourceApp {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    test08(env)

    env.execute(this.getClass.getSimpleName)
  }

  def test09(env: StreamExecutionEnvironment): Unit = {
    val source: DataStream[Long] = env.generateSequence(1, 100)

    source.print()
  }

  def test08(env: StreamExecutionEnvironment): Unit = {
    val source: DataStream[String] = env.fromElements[String]("a b c", "a b c")

    println(source.getParallelism) // 1

    source
      .flatMap((line: String, ctx: Collector[String]) => line.split(" ").foreach(ctx.collect))
      .map(new MapFunction[String, (String, Int)] {
        override def map(value: String): (String, Int) = (value, 1)
      })
      .keyBy(0)
      .reduce((t1, t2) => (t1._1, t1._2 + t2._2))
      .print
  }

  def test07(env: StreamExecutionEnvironment): Unit = {
    class AccessSourceV2 extends RichParallelSourceFunction[Access] {
      var running = true

      override def open(parameters: Configuration): Unit = println("~~~~~~open~~~~~~")

      override def run(ctx: SourceFunction.SourceContext[Access]): Unit = {
        val domains = Array[String]("com.com.lancer.com", "a.com", "b.com")
        while (running) {
          for (_ <- 1 to 10)
            ctx.collect(Access(1234567.toLong, domains(Random.nextInt(domains.length)), Random.nextDouble() + 1000))
          Thread.sleep(5000)
        }
      }

      override def cancel(): Unit = running = false
    }
    val source = env.addSource(new AccessSourceV2)
    println(source.getParallelism)
    source.print()
  }

  def test06(env: StreamExecutionEnvironment): Unit = {
    class AccessSourceV1 extends RichSourceFunction[Access] {

      var running = true

      override def open(parameters: Configuration): Unit = println("~~~~~~open~~~~~~")

      override def run(ctx: SourceFunction.SourceContext[Access]): Unit = {
        val domains = Array[String]("com.com.lancer.com", "a.com", "b.com")
        while (running) {
          for (_ <- 1 to 10)
            ctx.collect(Access(1234567.toLong, domains(Random.nextInt(domains.length)), Random.nextDouble() + 1000))
          Thread.sleep(5000)
        }
      }

      override def cancel(): Unit = running = false
    }
    val source = env.addSource(new AccessSourceV1)
    println(source.getParallelism)
    source.print()
  }

  def test05(env: StreamExecutionEnvironment): Unit = {
    val source = env.fromCollection(Array[Int](1, 2, 3))
    println(source.getParallelism) // 1
    source
      .map(i => i * 2)
      .print
  }

  def test04(env: StreamExecutionEnvironment): Unit = {
    val source = env.readTextFile("./data/access.log")
    println(source.getParallelism) // 16
  }

  def test03(env: StreamExecutionEnvironment): Unit = {
    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "bigdata01:9092,bigdata02:9092,bigdata03:9092,bigdata04:9092,bigdata05:9092")
    properties.setProperty("group.id", "test")
    val streamSource = env.addSource(new FlinkKafkaConsumer[String]("flinkTopic", new SimpleStringSchema(), properties))

    println("streamSource's parallelism：" + streamSource.getParallelism)
    streamSource.print()
  }

  def test02(env: StreamExecutionEnvironment): Unit = {
    val source = env.fromParallelCollection(new NumberSequenceIterator(1, 10))
    println("fromParallelCollection's parallelism：" + source.getParallelism) // 16

    val filterStream = source.filter(s => s >= 5)

    println("filterStream's parallelism：" + filterStream.getParallelism)
  }

  def test01(env: StreamExecutionEnvironment): Unit = {
    env.setParallelism(5); // 设置全局并行度

    // 底层都是使用StreamExecutionEnvironment.addSource(new xxxFunction() ---> who implements xxxSourceFunction<T>)
    // SourceFunction并行度为1（底层使用setParallelism(1)，故由优先级，全局并行度被覆盖），ParallelSourceFunction和RichParallelSourceFunction，可以指定并行度
    val source: DataStream[String] = env.socketTextStream("bigdata01", 9999); // .setParallelism(2); 报错，因为只能为1
    println("socketTextStream's parallelism：" + source.getParallelism)

    // 接收socket过来的数据，一行一个单词，把hello过滤掉
    val filterStream = source
      .filter(s => !s.equalsIgnoreCase("hello"))
      .setParallelism(1)

    println("filterStream's parallelism：" + filterStream.getParallelism)
  }
}
