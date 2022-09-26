package com.lancer.scala.sink

import com.lancer.scala.transformations.Access
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.redis.RedisSink
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig
import org.apache.flink.streaming.connectors.redis.common.mapper.{RedisCommand, RedisCommandDescription, RedisMapper}

object PKRedisSink {
  class RedisExampleMapper extends RedisMapper[(String, Double)]{
    override def getCommandDescription: RedisCommandDescription = {
      new RedisCommandDescription(RedisCommand.HSET, "HASH_NAME")
    }

    override def getKeyFromData(data: (String, Double)): String = data._1

    override def getValueFromData(data: (String, Double)): String = data._2.toString
  }

  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    val source: DataStream[String] = env.readTextFile("./data/access.log")

    val stream: DataStream[(String, Double)] = source
      .map[Access] {
        (line: String) => {
          val splits: Array[String] = line.split(",")
          Access(splits(0).trim.toLong, splits(1).trim, splits(2).trim.toDouble)
        }
      }
      .keyBy[String]((access: Access) => access.domain)
      .sum("traffic")
      .map[(String, Double)]((access: Access) => (access.domain, access.traffic))

    val conf = new FlinkJedisPoolConfig.Builder().setHost("bigdata01").build()

    stream.addSink(new RedisSink[(String, Double)](conf, new RedisExampleMapper))

    env.execute("PKRedisSInk")
  }
}
