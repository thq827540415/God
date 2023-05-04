package com.ivi.bigdata.spark.code.scala.sql

import com.ivi.code.util.SparkEnvUtil
import org.apache.spark.SparkConf
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions.{col, expr}
import org.apache.spark.sql.types.{DataTypes, StructField, StructType}

object DSLGrammar {
  def main(args: Array[String]): Unit = {

  }

  /**
   * DSL语法
   */
  def test18(): Unit = {
    val spark = SparkEnvUtil.getEnv()

    val schema = StructType(Seq(
      StructField("id", DataTypes.IntegerType),
      StructField("name", DataTypes.StringType)
    ))

    val df = spark.read.schema(schema).csv("./data/stu/input/b.txt")

    /**
     * 使用别名
     */
    df.select("id", "name").withColumnRenamed("id", "newId").withColumnRenamed("name", "newName").show(100, truncate = false)

    df.select("id", "name").toDF("newId", "newName").show(100, truncate = false)

    df.select(expr("id as newId"), expr("upper(name) as newName")).show(100, truncate = false)

    df.selectExpr("id + 1 as newId", "upper(name) as newName").show(100, truncate = false)

    import org.apache.spark.sql.functions._
    import spark.implicits._

    df.select($"id" + 1 as "newId", upper($"name") as "newName").show(100, truncate = false)

    df.select('id + 1 as "newId", upper('name) as "newName").show(100, truncate = false)

    df.select(col("id") + 1 as "newId", upper(df("name")).as("newName")).show(100, truncate = false)

    df.select(df("id").plus(1) <= 1 as "newId", upper(col("name")).as("newName")).show(100, truncate = false)

    df.select(col("id").plus(1) leq 1 as "newId", upper(df("name")) as "newName").show(100, truncate = false)

    /**
     * 过滤
     */
    // 底层使用filter算子
    df.where(($"id" > 1) and ($"id" leq 2)).show(100, truncate = false)

    df.where("id > 1 and id <= 2").show(100, truncate = false)

    // 算子层面
    df.filter("id > 1 and id <= 2").show(100, truncate = false)

    df.filter(row => row.getAs[Int]("id") > 1 && row.getAs[Int]("id") <= 2).show(100, truncate = false)

    /**
     * 分组 + 聚合，否则不能输出; 聚合：sum、min、max、count、avg,里面只能传字段名，不能传column对象
     *
     * 可以在agg里面使用聚合函数，否则，得先分组再聚合
     */
    // df.groupBy(upper('name) as "toUppercase").agg(sum("id") as "totalId")
    df.groupBy(upper($"name") as "toUppercase").sum("id").show(10, truncate = false)

    df.groupBy("name").count().show(10, truncate = false)

    // 全局直接聚合
    df.agg("id" -> "max", "id" -> "min").show(10, truncate = false)

    // 一个map中key相同的会被覆盖
    df.agg(Map("id" -> "max", "id" -> "sum")).show(10, truncate = false)

    df.agg(max("id") as "max", min($"id")).show(10, truncate = false)

    // 先分组，再聚合 --> 等价df.groupBy("name").max("id")
    df.groupBy("name").agg(max("id")).show(10, truncate = false)

    spark.close()
  }

  /**
   * join操作
   */
  def test20(): Unit = {
    val conf = new SparkConf().set("spark.default.parallelism", "1").set("com.lancer.scala.sql.shuffle.partitions", "1")
    val spark = SparkEnvUtil.getEnv(conf)

    val schemas = StructType(Seq(
      StructField("id", DataTypes.IntegerType),
      StructField("tall", DataTypes.IntegerType),
      StructField("height", DataTypes.IntegerType),
      StructField("face", DataTypes.IntegerType),
      StructField("gender", DataTypes.StringType)
    ))
    val df1 = spark.read.schema(schemas).csv("file:///Users/lancer/IdeaProjects/Spark/Spark-core/data/stu/input/a.txt")

    import spark.implicits._
    val df2 = spark.sparkContext.textFile("file:///Users/lancer/IdeaProjects/Spark/Spark-core/data/stu/input/b.txt")
      .map(line => {
        val arr = line.split(",")
        (arr(0).toInt, arr(1))
      })
      .toDF("id", "action")

    // 通过api进行join, 不指定连接条件就是使用的是笛卡尔积
    df1.join(df2).show(100, truncate = false)
    // cross join表示cartesian，笛卡尔积
    df1.crossJoin(df2).show(100, truncate = false)

    // 通过id字段进行join，前提是两张表都得有该join字段，右表的连接字段不会存在于结果表中
    df1.join(df2, "id").show(100, truncate = false)

    // 通过某些字段进行join，前提是两张表得有join的字段
    df1.join(df2, Seq("id")).show(100, truncate = false)

    // 通过column对象来指定字段进行join,如果是相同的字段，需要使用df的apply方法去生成column对象 --> 连接表达式, 会将所有字段显示出来，包括两个连接字段
    df1.join(df2, df1("id") + 1 === df2("id") && col("tall") >= 170).show(100, truncate = false)

    // 通过指定类型，来进行join，没指定连接类型和字段的，采用笛卡尔积；没指定连接类型，指定字段的，使用inner join
    df1.join(df2, df1("id") + 1 === df2("id") && col("face") >= 85, "right").show(100, truncate = false)

    // 使用left join时，连接条件所对应的条件是右表的，使用右表的数据去连接左表；right join使用的连接条件是左表对应的

    spark.close()
  }

  /**
   * 窗口函数操作：求每个城市中成绩最高的两个人的信息
   */
  def test21(): Unit = {
    val conf = new SparkConf().set("spark.default.parallelism", "1").set("com.lancer.scala.sql.shuffle.partitions", "1")
    val spark = SparkEnvUtil.getEnv(conf)

    import spark.implicits._

    val df = spark.sparkContext
      .parallelize(Seq(
        "1,张飞,21,M,北京,80",
        "2,关羽,23,M,北京,82",
        "7,周瑜,24,M,北京,85",
        "3,赵云,20,F,上海,88",
        "4,刘备,26,M,上海,83",
        "8,孙权,26,M,上海,78",
        "5,曹操,30,F,深圳,90.8",
        "6,孔明,35,F,深圳,77.8",
        "9,吕布,28,M,深圳,98"
      ))
      .map(record => {
        val arr = record.split(",")
        (arr(0).toInt, arr(1), arr(2).toInt, arr(3), arr(4), arr(5).toDouble)
      })
      .toDF("id", "name", "age", "sex", "city", "score")

    // 使用SQL语句
    df.createTempView("df")

    spark.sql(
      """
        |select id,
        |       name,
        |       age,
        |       sex,
        |       city,
        |       score
        |from (
        | select id,
        |        name,
        |        age,
        |        sex,
        |        city,
        |        score,
        |        row_number() over(partition by city order by score desc) as rk
        | from df
        |) t
        |where rk <= 2
        |
        |""".stripMargin).show(100, truncate = false)

    // 使用DSL语法
    import org.apache.spark.sql.functions._
    df.select($"id", $"name", $"age", $"sex", $"city", $"score",
      row_number() over (Window partitionBy "city" orderBy $"score".desc rowsBetween(Window.unboundedPreceding, Window.currentRow)) as "rk")
      .where($"rk" <= 2)
      .select("id", "name", "age", "sex", "city", "score")
      .show(100, truncate = false)

    spark.close()
  }
}
