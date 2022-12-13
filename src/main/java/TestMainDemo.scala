import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor, ValueStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeHint
import org.apache.flink.api.scala.scalaNothingTypeInfo
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.util.MathUtils

import java.sql.{DriverManager, Timestamp}
import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, Seq}

/**
 * @Author shadow
 * @Date 2022/8/4 23:53
 * @Description
 */
object TestMainDemo {
  def main(args: Array[String]): Unit = {
    // -372811216
    println(MathUtils.bitMix("abc".hashCode ^ "ns".hashCode))
    println(MathUtils.bitMix("abc".hashCode ^ "ns".hashCode))
    println(MathUtils.bitMix("abc".hashCode ^ "ns".hashCode))

    // 总能保证范围在127以内
    println(-372811216 & 127)

    println(Timestamp.valueOf("2022-10-26 11:57:05.111").getTime)
  }
}
