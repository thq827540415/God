package com.lancer.scala.broadcast

import org.apache.flink.api.common.state.MapStateDescriptor
import org.apache.flink.api.common.typeutils.base.{CharValueSerializer, IntSerializer}
import org.apache.flink.streaming.api.datastream.BroadcastStream
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.types.CharValue
import org.apache.flink.util.Collector

object BroadCastStreamTest {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    import org.apache.flink.streaming.api.scala._
    val genderStream: DataStream[(Int, Char)] = env
      .socketTextStream("bigdata01", 9999)
      .filter(_.trim.nonEmpty)
      .map(
        nowGenderInfo => {
          val arr = nowGenderInfo.split(",")
          val genderFlg = arr(0).trim.toInt
          val genderValue = arr(1).trim.charAt(0)
          (genderFlg, genderValue)
        })

    val stuInfoStream: DataStream[(Int, String, Int, String)] = env
      .socketTextStream("bigdata02", 9999)
      .filter(_.trim.nonEmpty)
      .map(
        nowStuInfo => {
          val arr = nowStuInfo.split(",")
          val id = arr(0).trim.toInt
          val name = arr(1).trim
          val genderFlg = arr(2).trim.toInt
          val address = arr(3).trim
          (id, name, genderFlg, address)
        }
      )

    val broadcastStateDescriptors: MapStateDescriptor[Integer, CharValue] = new MapStateDescriptor("bcGenderInfo", new IntSerializer, new CharValueSerializer)

    val bcDS: BroadcastStream[(Int, Char)] = genderStream.broadcast(broadcastStateDescriptors)

    val bcStream: BroadcastConnectedStream[(Int, String, Int, String), (Int, Char)] = stuInfoStream.connect(bcDS)

    val resultStream = bcStream.process(new MyBroadcastProcessFunction(broadcastStateDescriptors))

    resultStream.print("广播无界流的结果 ->")

    env.execute(this.getClass.getSimpleName)
  }

  private class MyBroadcastProcessFunction(broadcastStateDescriptors: MapStateDescriptor[Integer, CharValue])
    extends BroadcastProcessFunction[(Int, String, Int, String), (Int, Char), (Int, String, Char, String)] {
    override def processElement(value: (Int, String, Int, String), ctx: BroadcastProcessFunction[(Int, String, Int, String), (Int, Char), (Int, String, Char, String)]#ReadOnlyContext, out: Collector[(Int, String, Char, String)]): Unit = {
      val genderFlg = value._3

      val genderValueTmp: CharValue = ctx.getBroadcastState(broadcastStateDescriptors).get(genderFlg)
    }

    override def processBroadcastElement(value: (Int, Char), ctx: BroadcastProcessFunction[(Int, String, Int, String), (Int, Char), (Int, String, Char, String)]#Context, out: Collector[(Int, String, Char, String)]): Unit = ???
  }
}
