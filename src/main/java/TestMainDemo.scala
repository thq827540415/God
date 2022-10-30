import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.api.common.typeinfo.TypeHint
import org.apache.flink.runtime.state.{FunctionInitializationContext, FunctionSnapshotContext}
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.util.MathUtils

import java.sql.{DriverManager, Timestamp}
import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, Seq}

/**
 * @Author lancer
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

    class Demo extends RichMapFunction[String, String] with CheckpointedFunction {
      private var state: ListState[mutable.Map[String, Int]] = _

      override def map(value: String): String = {
        import scala.collection.JavaConverters._

        val list = new ListBuffer[mutable.Map[String, Int]]
        state.get().asScala.foreach(m => list += m)

        if (list.isEmpty) list += new mutable.HashMap[String, Int]

        val map = list.head
        map(value) = map.getOrElse(value, 0) + 1

        state.update(list.asJava)
        map.toList.sortBy(_._2).reverse.take(3).mkString(",")
      }

      override def snapshotState(context: FunctionSnapshotContext): Unit = ???

      override def initializeState(context: FunctionInitializationContext): Unit = {
        val descriptor = new ListStateDescriptor[mutable.Map[String, Int]](
          "test",
          new TypeHint[mutable.Map[String, Int]] {}.getTypeInfo)
        state = context.getOperatorStateStore.getListState(descriptor)
      }
    }
    val env = StreamExecutionEnvironment.getExecutionEnvironment


    import org.apache.flink.api.scala._

    /*env
      .socketTextStream("localhost", 9999)
      .assignTimestampsAndWatermarks(WatermarkStrategy.forMonotonousTimestamps().withTimestampAssigner(new SerializableTimestampAssigner[String] {
        override def extractTimestamp(element: String, recordTimestamp: Long): Long = {
          element.toLong
        }
      })).print()
    // .map(new Demo).setParallelism(1)

    env.execute()*/


    // Class.forName("ru.yandex.clickhouse.ClickHouseDriver")
    // println(DriverManager.getConnection("jdbc:clickhouse://localhost:8123/default", "default", ""))

    println(Timestamp.valueOf("2022-10-26 11:57:05.111").getTime)

  }
}
