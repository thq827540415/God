package com.shadow.garden.bigdata.spark.scala.core

import com.shadow.garden.bigdata.consts.MySQLConsts
import com.shadow.garden.bigdata.util.SparkEnvUtil
import org.apache.spark.rdd.JdbcRDD
import org.apache.spark.SparkConf

import java.sql.{DriverManager, ResultSet}

object E01_JdbcRDD {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("JdbcRDD").setMaster("local")
    val sc = SparkEnvUtil.getEnv(conf, enableHiveSupport = false).sparkContext

    val getConnection = () => DriverManager.getConnection(MySQLConsts.URL)

    val sql = "select * from person where age >= ? and age <= ?"

    val mapRow = (rs: ResultSet) => (rs.getString(1), rs.getInt(2))

    val jdbcRDD: JdbcRDD[(String, Int)] = new JdbcRDD[(String, Int)](sc, getConnection, sql, 20, 30, 2, mapRow)

    jdbcRDD.foreach(println)

    sc.stop()
  }
}
