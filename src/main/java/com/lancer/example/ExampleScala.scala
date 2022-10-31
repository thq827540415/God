package com.lancer.example

import com.fasterxml.jackson.databind.ObjectMapper
import com.lancer.utils.JsonUtils
import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}
import org.apache.flink.api.common.serialization.DeserializationSchema
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.scala._
import org.apache.flink.api.scala.typeutils.Types
import org.apache.flink.configuration.Configuration
import org.apache.flink.connector.kafka.source.KafkaSource
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.scala.function.{ProcessWindowFunction, WindowFunction}
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.streaming.connectors.redis.RedisSink
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig
import org.apache.flink.streaming.connectors.redis.common.mapper.{RedisCommand, RedisCommandDescription, RedisMapper}
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import org.apache.flink.util.Collector

import java.sql.Timestamp
import java.util.Objects


/**
 * ChangeRecord、ProduceRecord、EnvironmentData -> partitions 1
 */
object ExampleScala {

  // {"machine_id": "1", "product_id": "1", "product_status": "finished", "produce_time": "2022-10-31 14:01:00"}
  // {"machine_id": "1", "product_id": "1", "product_status": "finished", "produce_time": "2022-10-31 14:03:00"}
  // {"machine_id": "1", "product_id": "1", "product_status": "finished", "produce_time": "2022-10-31 14:05:00"}
  // {"machine_id": "1", "product_id": "1", "product_status": "finished", "produce_time": "2022-10-31 14:06:00"}
  // {"machine_id": "1", "product_id": "1", "product_status": "finished", "produce_time": "2022-10-31 14:07:00"}
  // {"machine_id": "1", "product_id": "1", "product_status": "finished", "produce_time": "2022-10-31 14:10:00"}
  case class Product(productId: String, productStatus: String, machineId: String, produceTime: Timestamp)

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    env.setMaxParallelism(4);

    val tableEnv = StreamTableEnvironment.create(env)

    doTask01(env)

    env.execute(ExampleScala.getClass.getSimpleName)
  }

  /**
   *
   * ProduceRecord
   */
  def doTask01(env: StreamExecutionEnvironment) = {

    /*env
      .fromSource(getKafkaSource("ProduceRecord"),
        WatermarkStrategy
          .forMonotonousTimestamps()
          .withTimestampAssigner(new SerializableTimestampAssigner[Product] {
            override def extractTimestamp(element: Product, recordTimestamp: Long): Long = {
              element.produceTime.getTime
            }
          }),
        "ProduceRecord")
      .filter("finished" == _.productStatus)
      .keyBy(_.productId)
      .window(TumblingEventTimeWindows.of(Time.minutes(5)))
      .process(new ProcessWindowFunction[Product, (String, String), String, TimeWindow] {
        var state: ValueState[Int] = _

        override def open(parameters: Configuration): Unit = {
            state = getRuntimeContext.getState(new ValueStateDescriptor[Int]("cnt", classOf[Int]))
        }

        override def process(key: String,
                             context: Context,
                             elements: Iterable[Product],
                             out: Collector[(String, String)]): Unit = {
          var i = state.value()
          if (Objects.isNull(i)) i = 0
          elements.foreach(_ => i += 1)
          state.update(i)
          out.collect((key, i.toString))
        }
      })
      .addSink(getRedisSink())*/
  }

  case class ChangeRecord(changedId: String, changeMachineId: String, changeMachineRecordId: String,
                          changeRecordState: String, changeStartTime: Timestamp, changeEndTime: Timestamp,
                          changeRecordData: String, changeHandleState: String)

  def doTask02(env: StreamExecutionEnvironment) = {

  }

  def getKafkaSource(topic: String): KafkaSource[ChangeRecord] = {
    KafkaSource.builder[ChangeRecord]()
      .setBootstrapServers(Consts.BOOTSTRAP_SERVER)
      .setTopics(topic)
      .setGroupId("test-group")
      .setProperty("auto.offset.reset", "latest")
      .setValueOnlyDeserializer(new DeserializationSchema[ChangeRecord] {
        override def deserialize(message: Array[Byte]): ChangeRecord = {
          import com.alibaba.fastjson.JSON
          JSON.parseObject(message, classOf[ChangeRecord])
          // JsonUtils.parseObject(new String(message), classOf[ChangeRecord])
        }

        override def isEndOfStream(nextElement: ChangeRecord): Boolean = false

        override def getProducedType: TypeInformation[ChangeRecord] = Types.POJO(classOf[ChangeRecord])
      })
      .build()
  }


  def getRedisSink(): RedisSink[(String, String)] = {
    new RedisSink(
      new FlinkJedisPoolConfig.Builder()
        .setHost(Consts.REDIS_HOST)
        .setPort(Consts.REDIS_PORT)
        .setDatabase(0)
        .build(),
      new RedisMapper[(String, String)] {
        override def getCommandDescription: RedisCommandDescription = {
          new RedisCommandDescription(RedisCommand.SET)
        }

        override def getKeyFromData(data: (String, String)): String = "totalproduce"

        override def getValueFromData(data: (String, String)): String = data.toString()
      }
    )
  }

  object Consts {
    val BOOTSTRAP_SERVER = "bigdata01:9092,bigdata02:9092,bigdata03:9092"

    val REDIS_HOST = "localhost"

    val REDIS_PORT = 6379
  }
}
