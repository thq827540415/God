import org.apache.flink.api.common.functions.RichMapFunction
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala._

/**
 * @Author lancer
 * @Date 2022/8/4 23:53
 * @Description
 */
object TestMainDemo {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(4)

    env.socketTextStream("bigdata01", 9999)
      .flatMap(_.split(" "))
      .map(new RichMapFunction[String, (String, Int)] {

        var subtask: Int = _

        override def open(parameters: Configuration): Unit = {
          subtask = getRuntimeContext.getIndexOfThisSubtask
        }

        override def map(value: String): (String, Int) = {
          (value, subtask)
        }
      })
      .rescale
      .addSink(new RichSinkFunction[(String, Int)] {
        var subtask: Int = _

        override def open(parameters: Configuration): Unit = {
          subtask = getRuntimeContext.getIndexOfThisSubtask
        }

        override def invoke(value: (String, Int), context: SinkFunction.Context): Unit = {
          println(s"$value =====>  $subtask")
        }
      }).setParallelism(3)
    env.execute(TestMainDemo.getClass.getSimpleName)
  }
}
