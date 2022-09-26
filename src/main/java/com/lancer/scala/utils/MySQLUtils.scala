package com.lancer.scala.utils

import java.sql.{Connection, DriverManager, PreparedStatement}

object MySQLUtils {
  private var connection: Connection = _

  def getConnection: Connection = {
    this.synchronized {
      if (connection == null) {
        connection = DriverManager.getConnection("jdbc:mysql://mysql:3306/flinkTest?useSSL=false", "root", "123456")
      }
      connection
    }
  }

  def close(connection: Connection, preparedStatement: PreparedStatement): Unit = {
    this.synchronized {
      if (connection != null) connection.close()
      if (preparedStatement != null) preparedStatement.close()
    }
  }
}
