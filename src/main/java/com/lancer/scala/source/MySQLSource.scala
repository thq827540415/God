package com.lancer.scala.source

import com.lancer.scala.utils.MySQLUtils
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.datastream.DataStreamSource
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.functions.source.{RichSourceFunction, SourceFunction}

import java.sql.{Connection, PreparedStatement, ResultSet}

object MySQLSource {

  class StudentSource extends RichSourceFunction[Student] {

    var connection: Connection = _
    var preparedStatement: PreparedStatement = _

    override def open(parameters: Configuration): Unit = {
      connection = MySQLUtils.getConnection
      preparedStatement = connection.prepareStatement("select * from student")
    }

    override def close(): Unit = MySQLUtils.close(connection, preparedStatement)

    override def run(ctx: SourceFunction.SourceContext[Student]): Unit = {
      val rs: ResultSet = preparedStatement.executeQuery()
      while (rs.next()) {
        ctx.collect(Student(rs.getInt("id"), rs.getString("name"), rs.getInt("age")))
      }
    }

    override def cancel(): Unit = ???
  }

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val source: DataStreamSource[Student] = env.addSource(new StudentSource)
    println(source.getParallelism)
    source.print()

    env.execute("MySQLSource")
  }
}

case class Student(id: Int, name: String, age: Int)
