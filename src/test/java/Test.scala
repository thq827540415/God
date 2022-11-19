
import com.shadow.garden.bigdata.util.SparkEnvUtil
import org.apache.commons.lang3.time.{DateFormatUtils, DateUtils}
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions.{col, expr}
import org.apache.spark.sql.types.{DataTypes, StructField, StructType}
import org.apache.spark.sql.{Row, SaveMode, SparkSession}
import org.apache.spark.{SparkConf, SparkContext, SparkFiles}
import org.junit.Test

import java.sql.DriverManager
import java.text.SimpleDateFormat
import java.util.{Calendar, Properties}
import scala.beans.BeanProperty
import scala.io.Source
import scala.language.postfixOps

object Test {
  def main(args: Array[String]): Unit = {
    println(Array(1, 2, 3).mkString("Array(", ", ", ")"))
    println(Array(1, 2, 3).mkString(","))
  }
}

class Test1 {
  @Test
  def test(): Unit = {
    val source = Source.fromFile("/Users/lancer/IdeaProjects/Spark/Spark-core/data/wordcount/input/a.txt", "UTF-8")
    val arr: Array[String] = source.getLines().toArray
    arr
      .filter(line => {
        val strings = line.split("\\s+")
        strings.size >= 8 && !strings(0).startsWith("h") && !strings.exists(_.length > 6)
      })
      .foreach(println)
  }

  @Test
  def test1(): Unit = {
    val arr = Array(1, 2, 3)
    println(arr.take(1).mkString(","))
    println(arr.last)
    println(arr.head)
    println(arr.tail.mkString(","))
  }

  @Test
  def test2(): Unit = {
    println(Array("1", 2, 3, 4).flatMap(_ => Seq(1, 2, 3)).mkString("Array(", ", ", ")"))
    Array(1, 2, 3, "1").iterator
  }

  @Test
  def test3(): Unit = {
    Logger.getLogger("org.apache.spark").setLevel(Level.ERROR)


    val conf = new SparkConf().setMaster("local").setAppName("this.getClass.getSimpleName")
    val sc = new SparkContext(conf)

    val source = sc.parallelize(Seq(
      "1 kill 3",
      "1 save 5",
      "2 kill 1",
      "2 save 5"
    ))

    source.mapPartitions(iter => {

      val conn = DriverManager.getConnection("jdbc:mysql://www.thqdomain.xyz:3306/test?useSSL=false&&characterEncoding=UTF-8", "root", "123456")
      val statement = conn.prepareStatement("select id, name, age from person where id = ?")

      iter.map(line => {
        val arr = line.split("\\s+")
        val id = arr(0).toInt
        statement.setInt(1, id)
        val rs = statement.executeQuery()
        rs.next()
        val name = rs.getString(2)
        val age = rs.getInt(3)
        line + " " + name + " " + age
      })
    }).foreach(println(_))

    source.map(line => {

      val conn = DriverManager.getConnection("jdbc:mysql://www.thqdomain.xyz:3306/test?useSSL=false&&characterEncoding=UTF-8", "root", "123456")
      val statement = conn.prepareStatement("select id, name, age from person where id = ?")

      val arr = line.split("\\s+")
      val id = arr(0).toInt
      statement.setInt(1, id)
      val rs = statement.executeQuery()
      rs.next()
      val name = rs.getString(2)
      val age = rs.getInt(3)
      line + " " + name + " " + age
    }).foreach(println(_))

    sc.stop()
  }

  @Test
  def test4(): Unit = {
    val conf = new SparkConf().setMaster("local").setAppName("this.getClass.getSimpleName")
    val sc = new SparkContext(conf)

    val sourceRDD = sc.makeRDD(List(1, 1, 2, 2), 2)

    val i = sourceRDD.aggregate(100)(_ + _, _ + _)
    println(i)

    sc.stop()
  }

  @Test
  def test5(): Unit = {

    val iterator = Iterator((1, "kill", 6, "a"), (1, "kill", 5, "a"), (1, "guanya", 3, "a"), (2, "kill", 12, "A"))
    val res = iterator
      .filter(_._2.equals("kill"))
      .toList
      .groupBy(_._4)
      .map(m => {
        (m._1, m._2.foldLeft(0)((t1, t2) => t1 + t2._3))
      })

      .maxBy(_._2)

    println(res)
  }


  @Test
  def test06(): Unit = {
    val conf = new SparkConf().setMaster("local").setAppName("this.getClass.getSimpleName")
    val sc = new SparkContext(conf)

    val rdd1 = sc.makeRDD(Seq(("a", 12), ("a", 13)))
    val rdd2 = sc.makeRDD(Seq(("a", "x"), ("b", "z")))

    rdd1.mapValues(_ + 1).foreach(println)

    rdd1.join(rdd2).foreach(println)

    sc.stop()
  }

  @Test
  def test07(): Unit = {

    val sumOp = () => print("this is sumOp")

    def sumOp1(): Unit = {
      println("this is sumOp1")
    }

    val sumOp2 = sumOp1 _

    def doIt(op: () => Unit, a: Double, b: Double) = {
      op()
      a + b
    }

    println(doIt(sumOp2, 1, 2))
  }

  @Test
  def test08(): Unit = {
    Logger.getLogger("org.apache.spark").setLevel(Level.ERROR)
    val conf = new SparkConf().setMaster("local").setAppName("this.getClass.getSimpleName")
    val sc = new SparkContext(conf)

    val source = sc.parallelize(Seq("a" -> 1, "b" -> 2, "a" -> 1))

    source.values.foreach(println)

    println(source.countByValue()) // 根据不同的kv对来统计每个kv对出现的次数

    println(source.countByKey()) // 通过key值来统计

    source.cache()

    sc.stop()
  }

  @Test
  def test09(): Unit = {
    Logger.getLogger("org.apache.spark").setLevel(Level.ERROR)
    val conf = new SparkConf().setMaster("local").setAppName("this.getClass.getSimpleName")
    val sc = new SparkContext(conf)

    val source = sc.parallelize(Seq(1 -> "a", 2 -> "b", 3 -> "c"))
    sc.addFile("data/stu/input/a.txt")

    source.map(line => {
      val name = SparkFiles.get("a.txt")
      println(name)
    }).collect()
    sc.stop()
  }

  @Test
  def test10(): Unit = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd")
    val date = sdf.parse("2017-6-13")
    sdf.getCalendar.setTime(date)
    println(sdf.getCalendar.get(Calendar.MONTH) + 1)
  }

  /**
   * 使用DateUtils、DateFormatUtils工具类
   */
  @Test
  def test11(): Unit = {
    println(DateUtils.parseDate("2017-06-12", "yyyy-MM-dd"))

    val calendar = Calendar.getInstance()
    calendar.setTime(DateUtils.parseDate("2017-06-12", "yyyy-MM-dd"))
    println(calendar.get(Calendar.MONTH + 1))

    println(DateUtils.RANGE_MONTH_MONDAY)
    println(DateUtils.MILLIS_PER_HOUR)
    val str = DateFormatUtils.format(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss.SSS")
    println(str)
  }


  /**
   * 在DataFrame中使用sql
   */
  @Test
  def test12(): Unit = {
    val conf = new SparkConf().setMaster("local")
    val spark = SparkEnvUtil.getEnv(conf, enableHiveSupport = false)

    import spark.implicits._

    val df = spark.sparkContext
      .parallelize(Seq(
        "guid01,2018-02-28",
        "guid01,2018-03-01",
        "guid01,2018-03-02",
        "guid01,2018-03-05",
        "guid01,2018-03-04",
        "guid01,2018-03-06",
        "guid01,2018-03-07",
        "guid02,2018-03-01",
        "guid02,2018-03-02",
        "guid02,2018-03-03",
        "guid02,2018-03-06"
      ))
      .map(line => {
        val arr = line.split(",")
        (arr(0), arr(1))
      })
      .toDF("guid", "dt")

    df.createTempView("db")

    // 可以在dataframe上使用sql
    spark.sql(
      """
        |with tmp as (
        | select guid, dt, row_number() over(partition by guid order by dt) rk
        | from db
        |)
        |
        |select
        | guid,
        | min(dt) as start_dt,
        | max(dt) as end_dt,
        | count(1) as days
        |from tmp
        |group by date_sub(dt, rk), guid
        |having count(1) >= 3
        |""".stripMargin).show()

    spark.close() // 底层使用spark.stop()
  }


  /**
   * 查询数据库使用SQL
   */
  @Test
  def test13(): Unit = {
    Logger.getLogger("org.apache.spark").setLevel(Level.ERROR)
    val spark = SparkSession.builder().master("local").appName("test13").getOrCreate()

    val df = spark.read.jdbc("jdbc:mysql://118.178.239.176:3306/test?useSSL=false&characterEncoding=UTF-8&useUnicode=true&user=root&password=123456", "sequence", new Properties())

    df.createTempView("tb")

    spark.sql(
      """
        |select
        | guid,
        | min(dt) as start_dt,
        | max(dt) as end_dt,
        | count(1) as days
        |from (
        | select
        |   guid,
        |   dt,
        |   row_number() over(partition by guid order by dt) as rk
        | from tb
        |) t
        |group by date_sub(dt, rk), guid
        |having count(1) >= 3
        |""".stripMargin).show()

    spark.stop()
  }

  @Test
  def test14(): Unit = {
    Logger.getLogger("org.apache.spark").setLevel(Level.ERROR)
    val spark = SparkSession.builder().master("local").appName("test14").getOrCreate()

    import spark.implicits._

    val df = spark.sparkContext
      .parallelize(Seq(
        "1,2020-02-18 14:20:30,2020-02-18 14:46:30,20",
        "1,2020-02-18 14:47:20,2020-02-18 15:20:30,30",
        "1,2020-02-18 15:37:23,2020-02-18 16:05:26,40",
        "1,2020-02-18 16:06:27,2020-02-18 17:20:49,50",
        "1,2020-02-18 17:21:50,2020-02-18 18:03:27,60",
        "2,2020-02-18 14:18:24,2020-02-18 15:01:40,20",
        "2,2020-02-18 15:20:49,2020-02-18 15:30:24,30",
        "2,2020-02-18 16:01:23,2020-02-18 16:40:32,40",
        "2,2020-02-18 16:44:56,2020-02-18 17:40:52,50",
        "3,2020-02-18 14:39:58,2020-02-18 15:35:53,20",
        "3,2020-02-18 15:36:39,2020-02-18 15:24:54,30"
      ))
      .map(line => {
        val arr = line.split(",")
        (arr(0).toInt, arr(1), arr(2), arr(3).toInt)
      })
      .toDF("uid", "start_time", "end_time", "num")

    df.createTempView("inInterval")

    val sql =
      """
        |with tmp as (
        | select
        |   uid,
        |   start_time,
        |   end_time,
        |   num,
        |   lag(end_time, 1, null) over(partition by uid order by start_time) as pre_end_time
        | from inInterval
        |)
        |
        |select
        | uid,
        | min(start_time) as start_time,
        | max(end_time) as end_time,
        | sum(num) as amount
        |from (
        | select uid,
        |   start_time,
        |   end_time,
        |   num,
        |   sum(flag) over(partition by uid order by start_time) as groupId
        | from (
        |   select
        |     uid,
        |     start_time,
        |     end_time,
        |     num,
        |     if(unix_timestamp(start_time, 'yyyy-MM-dd HH:mm:ss') - nvl(unix_timestamp(pre_end_time), unix_timestamp(start_time)) < 10 * 60, 0, 1) as flag
        |   from tmp
        | ) t1
        |)t2
        |group by groupId, uid
        |""".stripMargin

    spark.sql(sql).show()

    df.write.mode(SaveMode.Overwrite).jdbc("jdbc:mysql://bigdata03:3306/test?useSSL=false&characterEncoding=UTF-8&useUnicode=true&user=root&password=123456", "inInterval", new Properties())

    spark.stop()
  }

  /**
   * 给普通类的字段，通过注解的方式，添加getter和setter方法
   */
  @Test
  def test17(): Unit = {
    class Main(
                @BeanProperty
                val name: String
              ){
    }
  }

  /**
   * DSL语法
   */
  @Test
  def test18(): Unit = {
    val spark = SparkEnvUtil.getEnv()

    val schema = StructType(Seq(
      StructField("id", DataTypes.IntegerType),
      StructField("name", DataTypes.StringType)
    ))

    val df = spark.read.schema(schema).csv("file:///Users/lancer/IdeaProjects/Spark/Spark-core/data/stu/input/b.txt")

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
   * 子查询
   */
  @Test
  def test19(): Unit = {
    val spark = SparkEnvUtil.getEnv()

    import spark.implicits._

    val df = spark.sparkContext
      .parallelize(Seq(
        "shanghai,98",
        "beijing,95",
        "shanghai,99"
      ))
      .map(line => {
        val arr = line.split(",")
        (arr(0), arr(1).toDouble)
      })
      .toDF("city", "score")


    import org.apache.spark.sql.functions._

    /**
     * 用api实现子查询
     * select * from (
     * select
     * city,
     * sum(score) as total
     * from t
     * group by city
     * ) o
     * where total >= 100
     */
    df.groupBy("city")
      .agg(sum("score") as "total")
      .where($"total" geq 100)
      .show(100, truncate = false)

    spark.close()
  }

  /**
   * join操作
   */
  @Test
  def test20(): Unit = {
    val conf = new SparkConf().set("spark.default.parallelism", "1").set("com.lancer.scala.sql.shuffle.partitions", "1")
    val spark = SparkEnvUtil.getEnv(conf).asInstanceOf[SparkSession]

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

    // 通过api进行join, 默认使用的是笛卡尔积
    df1.join(df2).show(100, truncate = false)
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
  @Test
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
        |select
        | id,
        | name,
        | age,
        | sex,
        | city,
        | score
        |from (
        | select
        |  id,
        |  name,
        |  age,
        |  sex,
        |  city,
        |  score,
        |  row_number() over(partition by city order by score desc) as rk
        | from df
        |) t
        |where rk <= 2
        |
        |""".stripMargin).show(100, truncate = false)

    // 使用DSL语法
    import org.apache.spark.sql.functions._
    df.select($"id", $"name", $"age", $"sex", $"city", $"score", row_number() over(Window partitionBy "city" orderBy $"score".desc rowsBetween(Window.unboundedPreceding, Window.currentRow)) as "rk")
      .where($"rk" <= 2)
      .select("id", "name", "age", "sex", "city", "score")
      .show(100, truncate = false)

    spark.close()
  }

  /**
   * 将DF转成RDD[Tuple]的方式
   */
  @Test
  def test22(): Unit = {
    val conf = new SparkConf().set("spark.default.parallelism", "1").set("com.lancer.scala.sql.shuffle.partitions", "1").setMaster("local[*]")
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

    /**
     * 模式匹配
     * 求每个城市中成绩最高的两个人的信息
     */
    df.rdd
      .map {
        case Row(id: Int, name: String, age: Int, sex: String, city: String, score: Double) =>
          (id, name, age, sex, city, score)
      }
      .groupBy(_._5)
      .mapValues(iter => {
        iter
          .toList
          .sortBy(_._6)
          .reverse
          .take(2)
      })
      .foreach(_._2.foreach(println(_)))

    /**
     * 通过Row对象api
     * 求每种性别的成绩总和
     */
    df.rdd
      .map(row => {
        val id = row.get(0).asInstanceOf[Int]
        val name = row.getString(1)
        val age = row.getAs[Int](2)
        val sex = row.getAs[String]("sex")
        val city = row.getAs[String]("city")
        val score = row.getDouble(row.size - 1)
        (id, name, age, sex, city, score)
      })
      .groupBy(_._4)
      .mapValues(iter => {
        /*iter.aggregate(0.0)((v, tp) => {
          v + tp._6
        }, _ + _)*/
        iter.map(_._6).sum
      })
      .foreach(println)

    /**
     * 用DSL求每种性别的成绩总和
     */
    import org.apache.spark.sql.functions._
    df.groupBy("sex")
      .agg(sum("score") as "total")
      .show(100, truncate = false)

    /**
     * 用SQL求每种性别的成绩总和
     */
    df.createTempView("df")
    spark.sql(
      """
        |
        |select
        | sex,
        | sum(score) as total
        |from df
        |group by sex
        |
        |""".stripMargin).show(10, truncate = false)

    spark.close()
  }
}
