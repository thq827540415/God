package com.lancer.bigdata.hudi

import com.lancer.bigdata.util.SparkEnvUtil
import org.apache.spark.SparkConf

object HudiConfigure {

  // System.setProperty("HUDI_CONF_DIR", "hdfs://bigdata01:9000/")

  def main(args: Array[String]): Unit = {

    val conf = new SparkConf()
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      // 导入，则可以使用SQL方式来创建表
      .set("spark.sql.extensions", "org.apache.spark.sql.hudi.HoodieSparkSessionExtension")
      .setMaster("local")

    val spark = SparkEnvUtil.getEnv(conf)



    spark.sql(
      """
        |create table person(
        |   name string,
        |   age string,
        |   dt string
        |) using hudi
        |partitioned by (age, dt)
        |tblproperties(
        |   type = 'cow',
        |   primaryKey = 'name',
        |   preCombineField = 'dt'
        |)
        |location 'hdfs://bigdata01:9000/user/hive/warehouse'
        |""".stripMargin)

    /*spark.sql(
      """
        |insert into table person(name, age, dt) values ('zs', '18', '2022-11-04')
        |""".stripMargin)*/


    /*val df = spark.read.json("/test.json")

    val theOptions = Map(
      // 指定record key
      DataSourceWriteOptions.RECORDKEY_FIELD.key() -> "id",
      // 指定partition path，若不指定分区，则默认只有一个分区default
      DataSourceWriteOptions.PARTITIONPATH_FIELD.key() -> "data_dt",
      // 指定表名，只是用于存在HMS中
      HoodieWriteConfig.TBL_NAME.key() -> "test",
      // 指定表类型，必须要大写
      DataSourceWriteOptions.TABLE_TYPE.key() -> "COPY_ON_WRITE",
      // 指定进行的操作是upsert
      DataSourceWriteOptions.OPERATION.key() -> "upsert",
      // 主键相同，留哪一条; 按照时间大的留谁
      DataSourceWriteOptions.PRECOMBINE_FIELD.key() -> "data_dt",
    )

    df.write.format("org.apache.hudi")
      .options(QuickstartUtils.getQuickstartWriteConfigs)
      .options(theOptions)
      // Overwrite若该路径存在文件，则将该路径全部覆盖后，再进行hudi的operation
      // Append若该路径存在文件，则在该路径下进行hudi的operation
      .mode(SaveMode.Append)
      .save("/hudi/test")*/

    spark.stop()
  }
}
