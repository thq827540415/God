package com.lancer.scala.partitioner

import com.lancer.scala.transformations.Access
import org.apache.flink.api.common.functions.Partitioner
import org.apache.flink.streaming.api.functions.source.{ParallelSourceFunction, SourceFunction}
import org.apache.flink.streaming.api.scala._

import scala.util.Random

object PartitionerApp {
  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    test01(env)

    env.execute("PartitionerApp")
  }

  def test01(env: StreamExecutionEnvironment): Unit = {
    env.setParallelism(3)

    class AccessSource extends ParallelSourceFunction[Access] {

      private var running: Boolean = true

      override def run(ctx: SourceFunction.SourceContext[Access]): Unit = {
        val domains: Array[String] = Array[String]("com.com.lancer.com", "a.com", "c.com")
        while (running) {
          for (_ <- 1 to 10)
            ctx.collect(Access(1234567.toLong, domains(Random.nextInt(domains.length)), Random.nextDouble() + 1000))
          Thread.sleep(1000)
        }
      }

      override def cancel(): Unit = running = false
    }

    class PKPartitioner extends Partitioner[String] {
      override def partition(key: String, numPartitions: Int): Int = {
        if ("com.com.lancer.com".equals(key)) {
          0
        } else if ("a.com".equals(key)) {
          1
        } else {
          2
        }
      }
    }

    val source: DataStream[Access] = env.addSource(new AccessSource)
    val transformStream = source
      .map[(String, Access)]((access: Access) => (access.domain, access))
      .partitionCustom[String](new PKPartitioner, (t: (String, Access)) => t._1)
      .map[Access]((t: (String, Access)) => {
        print(s"current thread id is:  ${Thread.currentThread().getId}, value is:  ${t._2}")
        t._2
      })
    transformStream.print()
  }
}
