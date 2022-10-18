package com.lancer.scala.sink

import org.apache.flink.streaming.api.scala._

object SinkApp {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val source = env.socketTextStream("bigdata01", 9999);

    println(source.getParallelism)

    // 没有“>”符号的输出
    source.print().setParallelism(1);

    // “1>”符号前面加上test ---->  test:1> abc
    // 并行度为1   ----> test> abc
    source.print("test").setParallelism(2);

    env.execute("SinkApp")
  }
}
