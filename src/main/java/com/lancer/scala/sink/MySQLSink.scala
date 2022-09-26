package com.lancer.scala.sink

import com.lancer.scala.transformations.Access
import com.lancer.scala.utils.MySQLUtils
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.flink.streaming.api.scala._

import java.sql.{Connection, PreparedStatement}


object MySQLSink {

  class AccessSink extends RichSinkFunction[(String, Double)] {

    var connection: Connection = _
    var insertPreparedStatement: PreparedStatement = _
    var updatePreparedStatement: PreparedStatement = _

    override def open(parameters: Configuration): Unit = {
      connection = MySQLUtils.getConnection
      insertPreparedStatement = connection.prepareStatement("insert into traffic(domain, traffic) values(?, ?)")
      updatePreparedStatement = connection.prepareStatement("update traffic set traffic = ? where domain = ?")
    }

    override def close(): Unit = {
      MySQLUtils.close(connection, insertPreparedStatement);
      MySQLUtils.close(connection, updatePreparedStatement);
    }

    override def invoke(value: (String, Double), context: SinkFunction.Context): Unit = {
      updatePreparedStatement.setDouble(1, value._2)
      updatePreparedStatement.setString(2, value._1)
      updatePreparedStatement.execute()
      if (updatePreparedStatement.getUpdateCount == 0) {
        insertPreparedStatement.setString(1, value._1)
        insertPreparedStatement.setDouble(2, value._2)
        insertPreparedStatement.execute()
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val env: StreamExecutionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment

    env.setParallelism(1)

    val source: DataStream[String] = env.readTextFile("./data/access.log")

    val transformStream: DataStream[(String, Double)] = source
      .map[Access] {
        (line: String) => {
          val splits: Array[String] = line.split(",")
          Access(splits(0).trim.toLong, splits(1).trim, splits(2).trim.toDouble)
        }
      }
      .keyBy[String]((access: Access) => access.domain)
      .sum("traffic")
      .map[(String, Double)]((access: Access) => (access.domain, access.traffic))

    // transformStream.print()

    transformStream.addSink(new AccessSink)

    env.execute("MySQLSink");
  }
}
