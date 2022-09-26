package com.lancer.scala.broadcast

import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.api.scala.ExecutionEnvironment
import org.apache.flink.configuration.Configuration

import java.util

object BroadCastDSTest {
  def main(args: Array[String]): Unit = {
    val env = ExecutionEnvironment.getExecutionEnvironment

    import org.apache.flink.api.scala._

    val genderInfoDS: DataSet[(Int, Char)] = env.fromElements(
      (1, '男'),
      (2, '女'),
      (3, 'x')
    )
    val studentInfoDS: DataSet[(Int, String, Int, String)] = env.fromElements(
      (101, "独孤求败", 1, "华山之巅"),
      (108, "楚楚动人", 2, "杭州西湖畔"),
      (119, "你猜", 3, "昆仑山脉")
    )
    // 将数据量小的数据集封装成广播变量，用来分析数据量大的数据集
    val broadcastName: String = "genderInfo"

    val resultDS: DataSet[(Int, String, Char, String)] = studentInfoDS
      .map(
        new MyRichMapFunction(broadcastName)
      )
      .withBroadcastSet(genderInfoDS, broadcastName)

    resultDS.print()
  }

  private class MyRichMapFunction(broadcastName: String)
    extends RichMapFunction[(Int, String, Int, String), (Int, String, Char, String)] {

    var broadcastInfo: Map[Int, Char] = _

    override def open(parameters: Configuration): Unit = {
      /**
       * scala.collection.Seq  ==> java.util.List
       * scala.collection.mutable.Seq  ==> java.util.List
       * scala.collection.Set  ==> java.util.Set
       * scala.collection.Map ==> java.util.Map
       * java.util.Properties  ==> scala.collection.mutable.Map[String, String]
       */
      import scala.collection.JavaConversions._
      val lst: util.List[(Int, Char)] = this.getRuntimeContext.getBroadcastVariable("genderInfo")
      val broadcastInfoTmp: Seq[(Int, Char)] = lst
      broadcastInfo = broadcastInfoTmp.toMap
    }

    override def close(): Unit = super.close()

    override def map(value: (Int, String, Int, String)): (Int, String, Char, String) = {
      val genderFlg: Int = value._3
      val gender = broadcastInfo(genderFlg)

      (value._1, value._2, gender, value._4)
    }
  }

}
