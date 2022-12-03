package com.shadow.garden.bigdata.spark.code.scala.sql

import com.shadow.garden.bigdata.consts.Consts
import com.shadow.garden.bigdata.util.SparkEnvUtil
import org.apache.spark.SparkConf
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.types.{DataTypes, StructField, StructType}
import org.apache.spark.sql.{Row, SparkSession}

import java.util.Properties
import scala.collection.immutable.HashMap


object E01_BasicReadApi {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("basic read")
    val spark = SparkEnvUtil.getEnv(conf, enableHiveSupport = false)
    readFromJson(spark)
    spark.close()
  }

  /**
   * 从csv文件读数据
   */
  private def readFromCsv(spark: SparkSession): Unit = {
    // 手动指定schema
    val schemas = StructType(
      StructField("id", DataTypes.LongType)
        :: StructField("tall", DataTypes.IntegerType)
        :: StructField("weight", DataTypes.IntegerType)
        :: StructField("face", DataTypes.IntegerType)
        :: StructField("gender", DataTypes.StringType)
        :: Nil)

    // DataFrame是Dataset[Row]的别名，其上还可以调用RDD算子
    spark.read
      // 告诉它数据中包含头
      .option("header", "true")
      // 自动推断类型，不要使用，会额外触发join
      .option("inferSchema", "false")
      // 指定分割字段
      .option("seq", ",")
      // 手动指定Schema（字段名 + 字段类型），如果指定了头，那么按照手动 指定的schema来
      .schema(schemas)
      .csv("data/test.csv")
      .show(100, truncate = false)

    spark.sql("select * from csv.`data/test.csv`").show(100, truncate = false)
  }

  /**
   * 从json文件中读取数据
   */
  private def readFromJson(spark: SparkSession): Unit = {
    // 解析json数据，将解析失败的数据放入名为_corrupt_record字段中，将解析出来的key作为字段名
    val df = spark.read.json("data/test.txt")
    import spark.implicits._

    /**
     * 不能直接引用_corrupt_record字段，所以不能使用SQL去过滤脏数据，调用api去过滤
     * 过滤出解析成功的字段, 只有存在错误的数据, 才有_corrupt_record字段
     * 可以使用try catch，来判断是否有异常
     */
    // df.filter(column("_corrupt_record").isNull) // 直接传入column方法

    // 如果没有错误数据，直接返回原数据
    var df2 = df
    try {
      // $/'表示把字段作为Column对象传入，新版本中，只能引用数据中的解析出来的字段，不能引用_corrupt_record字段
      df2 = df.filter('_corrupt_record.isNull)
    } catch {
      case _: Exception => println("没有错误数据，没有_corrupt_record列")
    }

    /**
     * {"name": "zs", "age": 18, "family": [{"name": "aa", "relationship":"mother", "hobby":
     * [{"playBasketball": true, "playFootball": false}]}, {"name": "bb", "relationship": ""}]}
     * 会按照最复杂的那条json来确定结构，也可以手动指定schema，嵌套的{}结构默认都是struct类型，会出现大量空值(老版本为空串，新版本中改成null);
     * []结构都是array类型
     *
     * root
     * |-- _corrupt_record: string (nullable = true) -- 行中包含错误数据
     * |-- age: long (nullable = true)
     * |-- family: array (nullable = true)
     * |    |-- element: struct (containsNull = true)
     * |    |    |-- age: long (nullable = true)
     * |    |    |-- hobby: array (nullable = true)
     * |    |    |    |-- element: struct (containsNull = true)
     * |    |    |    |    |-- playBasketball: boolean (nullable = true)
     * |    |    |    |    |-- playFootball: boolean (nullable = true)
     * |    |    |-- name: string (nullable = true)
     * |    |    |-- relationship: string (nullable = true)
     * |-- name: string (nullable = true)
     *
     * SQL中(array+struct)复杂数据获取：family[0].hobby[0].playBasketball --> 母亲是否打篮球
     * (array+Map) : family[0].hobby['playBasketball']
     */

    df2.show(100, truncate = false)
    df2.printSchema()


    /**
     * 手动指定嵌套类型，避免大量空值的产生
     */
    val df3 = spark.read
      .schema(
        StructType(
          StructField("name", DataTypes.StringType)
            :: StructField("info", DataTypes.createMapType(DataTypes.StringType, DataTypes.BooleanType))
            :: Nil))
      .json("data/test.txt")
    df3.show(100, truncate = false)
    df3.printSchema()
  }

  /**
   * 使用JDBC读取数据
   */
  private def readFromJdbc(spark: SparkSession): Unit = {
    val df1 = spark.read.jdbc(Consts.MYSQL_URL, "test", new Properties())

    // 关联数据第一种方式 -- 广播变量实现map join
    // 学习中这样使用，平常数据都不是从本地读取, 可以读取数据到driver的RDD或者DF/DS，然后转成本地集合，发送到Executor端
    val mp = HashMap[Int, String](1 -> "scan", 2 -> "shopping", 3 -> "gaming") // 小表数据

    val bc = spark.sparkContext.broadcast(mp) // 将数据广播出去

    // 返回Row对象，需要传入Row对象的编码器：RowEncoder，其他对象，需要对应的Encoder
    // 指定字段的格式信息，用于将返回的Row进行序列化和反序列化
    val encoder = RowEncoder(
      StructType(Seq(
        StructField("uid", DataTypes.IntegerType),
        StructField("name", DataTypes.StringType),
        StructField("start_time", DataTypes.StringType),
        StructField("end_time", DataTypes.StringType),
        StructField("num", DataTypes.IntegerType)
      )))

    // 调用RDD算子，是先变成RDD，然后通过隐式转换中的Encoder，变成DF/DS
    df1.mapPartitions(
      iter => {
        val smallTable = bc.value
        iter.map(
          row => {
            val uid = row.getAs[Int]("uid")
            val start_time = row.getAs[String]("start_time")
            val end_time = row.getAs[String]("end_time")
            val num = row.getAs[Int]("num")
            val name = smallTable.getOrElse(uid, "未知")
            Row(uid, name, start_time, end_time, num)
          })
      })(encoder)
      .show(100, truncate = false)


    import spark.implicits._

    // 返回tuple，基本类型都是默认使用隐式转换中的ExpressionEncoder
    df1.mapPartitions(
      iter => {
        val smallTable = bc.value
        iter.map(
          row => {
            val uid = row.getAs[Int]("uid")
            val start_time = row.getAs[String]("start_time")
            val end_time = row.getAs[String]("end_time")
            val num = row.getAs[Int]("num")
            val name = smallTable.getOrElse(uid, "未知")
            // 由数据类型指定字段类型
            (uid, name, start_time, end_time, num)
          })
      })
      .toDF("uid", "name", "start_time", "end_time", "num").show(100, truncate = false)


    /**
     * 关联数据第二种方式 -- 两个df写sql实现join
     */
    val df2 = spark
      .read
      .schema(
        StructType(Seq(
          StructField("id", DataTypes.IntegerType),
          StructField("name", DataTypes.StringType)
        )))
      .csv("Spark-core/data/stu/input/b.txt")

    // 将两个df转成临时视图
    df1.createTempView("df1")
    df2.createTempView("df2")

    spark.sql(
      """
        |select uid,
        |       name,
        |       start_time,
        |       end_time,
        |       num
        |from df1 left join df2 on df1.uid = df2.id
        |
        |""".stripMargin).show(100, truncate = false)
  }
}
