### 一、Kappa架构（业界最常用的是Flink + Kafka）的痛点

1. Kafka无法支持海量数据存储
2. Kafka无法支持高效的OLAP查询
3. 无法复用目前已经非常成熟的基于离线数仓的数据血缘、数据质量管理体系
4. Kafka仅支持append

### 二、为什么要用数据湖技术？

1. 为了解决Kappa架构的痛点问题，业界最主流是采用“批流一体”方式，这里批流一体可以理解为批和流使用SQL统一处理，也可以理解为处理框架的统一，
例如：Spark、Flink，但这里更重要指的是存储层上的统一，只要存储层面上做到“批流一体”，就可以解决以上Kappa遇到的各种问题。
数据湖技术可以更好的实现存储层面上的“批流一体”，这就是为什么大数据中需要数据湖的原因。

### 三、Iceberg数据存储格式

1. 术语https://iceberg.apache.org/terms/

![Hive整合Iceberg文件目录结构](../../../../../../image/Hive整合Iceberg文件目录结构.png?msec=1667553515567)

![Iceberg查询最新数据](../../../../../../image/Iceberg查询最新数据.png?msec=1667553515566)

1. data files（数据文件）
   1. 数据文件是Iceberg表真实存储数据的文件，一般是在表的数据存储目录的data目录下，Iceberg每次更新会产生多个数据文件
2. Snapshot（表快照）
   1. 快照代表一张表在某个时刻的状态。每个快照里面会列出表在某个时刻的所有data files列表。data files是存储在不同的manifest files里面，manifest files是存储在一个Manifest list文件里面，而一个Manifest list文件代表一个当前快照
   2. 在Spark和Flink中，指定"snapshot-id"，可以查询历史上任何时刻的快照，如：`spark.read.option("snapshot-id", ....).format("iceberg").load("path")`
   3. 指定"as-of-timestamp"，来读取某个快照数据：`spark.read.option("as-of-timestamp", "时间戳").format("iceberg").load("path")`
3. Manifest list（清单列表）
   1. 每生成一个data files文件，就对应生成一个manifest file文件，并且生成一个快照manifest list，用于保存到当前所有的manifest file
   2. manifest list是一个元数据文件。这个元数据文件中存储的是Manifest file列表，每个Manifest file占据一行。每行中存储了Manifest file的路径、其存储的数据文件（data files）的分区范围，增加了几个数据文件、删除了几个数据文件等信息，这些信息可以用来在查询时提供过滤，加快速度
   3. deleted_data_files_count属性对应的int值大于0，表示数据删除，读数据时就无需读这个manifest清单文件对应的数据文件
4. Manifest file（清单文件）
   1. manifest file也是一个元数据文件，它列出组成快照的数据文件的列表信息。每行都是每个数据文件的详细描述，包括数据文件的状态、文件路径、分区信息、列级别的统计信息（比如每列的最大最小值、空值数等）、文件的大小、以及文件里面数据行数等信息。其中列级别的统计信息可以在扫描表数据时过滤掉不必要的文件。和data file一对一
   2. 当status属性为1，表示对应的parquet文件为新增文件，需要读取，为2表示parquet文件被删除
2. 表格式（Table Format）https://iceberg.apache.org/spec/

   1. Iceberg是一种用于大型数据分析数据集的开放表格式，表格式可以理解为元数据及数据文件的一种组织方式。
2. Iceberg表格式处于底层存储（HDFS、S3等）和上层计算框架（Flink、Spark等）之间

### 四、Iceberg特点

1. Iceberg分区与隐藏分区https://iceberg.apache.org/docs/latest/partitioning/
   1. Iceberg支持分区来加快数据查询。在Iceberg设置分区后，可以在写入数据时，将相似的行分组，在查询时加快查询速度。Iceberg中可以按照年月日和小时粒度划分时间戳组织分区。
   2. 在Hive中也支持分区，但是要想使分区能加快查询速度，需要在写SQL时指定对应的分区条件来过滤数据，在Iceberg中写SQL查询时，不需要在SQL中特别指定分区过滤条件，Iceberg会自动分区，过滤掉不需要的数据
   3. 在Iceberg中分区信息可以被隐藏起来，Iceberg的分区字段可以通过一个字段计算出来，在建表或者修改分区策略之后，新的数据会自动计算所属于的分区，在查询的时候同样不用关心表的分区是什么字段，只需要关注业务逻辑，Iceberg会自动过滤不需要的分区数据
   4. 正是由于Iceberg的分区信息和表数据存储目录是独立的，使得Iceberg的表分区可以被修改，而且不会涉及到数据迁移
2. 表演化（Table Evolution）
   1. 在Hive分区表中，如果把一个按照天分区的表改成按小时分区，那么没有办法在原有表上进行修改，需要创建一个按照小时分区的表，然后把数据加载到此表中
   2. Iceberg支持就地表演化，可以通过SQL的方式进行表级别模式演化，例如：更改表分区布局。Iceberg进行以上操作时，代价极低，不存在读出数据重新写入或者迁移数据这种费时费力的操作
3. 模式演化（Schema Evolution）https://iceberg.apache.org/docs/latest/evolution/
   1. Iceberg支持一下几种Schema的演化
      1. add：向表或者嵌套结构增加新列
      2. drop：从表或嵌套结构中移除列
      3. rename：重命名表中或者嵌套结构中的列
      4. update：将复杂结构（Struct、Map、List）中的基本类形扩展类型长度，比如：tinyint修改成int
      5. reorder：改变列的顺序，也可以改变嵌套结构中字段的排序顺序
   2. 注意
      1. Iceberg Schema的改变只是元数据的操作改变，不会涉及到重写数据文件。Map结构类型不支持add和drop字段
      2. Iceberg保证Schema演化是没有副作用的独立操作，不会涉及到重写数据文件，具体如下：
         1. 增加列时，不会从另一个列中读取已存在的数据
         2. 删除、更新列或者嵌套结构中的字段时，不会改变任何其他列的值
         3. 改变列或者嵌套结构中字段顺序的时候，不会改变相关联的值
   3. Iceberg实现以上的原因是，使用唯一的id来追踪表中的每一列，当添加一列时，会分配新的id，因此列对应的数据不会被错误使用
4. 分区演化（Partition Evolution）
   1. Iceberg分区可以在现有表中更新，因为Iceberg查询流程并不和分区信息直接关联
   2. 当我们改变一个表的分区策略时，对应修改分区之前的数据不会改变，依然会采用老的分区策略，新的数据会采用新的分区策略，也就是说同一个表会有两种分区策略，旧数据采用旧分区策略，新数据采用新分区策略，在元数据里两个分区策略相互独立，不重合
   3. 一次我们在写SQL进行数据查询时，如果存在跨分区策略的情况，则会解析成两个不同执行计划
5. 列顺序演化（Sort Order Evolution）

### 五、数据类型

https://iceberg.apache.org/docs/latest/schemas/

### 六、Hive和Iceberg整合

1. https://iceberg.apache.org/docs/latest/hive/

2. 下载iceberg-hive-runtime.jar https://iceberg.apache.org/releases/

3. 当jar包放在$HIVE_HOME/lib下时，则不用引入jar，即可创建iceberg表，在插入数据的时候才要引入；否则，需要在客户端使用add jar path引入jar，才能使用

4.     iceberg.engine.hive.enabled=true

5. Hive中创建表时，指定`stored by org.apache.iceberg.mr.hive.HiveIcebergStorageHandler`，表示创建在iceberg上

   1. 通过设置`set iceberg.catalog.<catalog_name>.type=hive`，指定iceberg使用名为'<catalog_name>'的HiveCatalog来管理元数据，在建表的时候，通过`tblproperties('iceberg.catalog' = '<catalog_name>')`来指定表使用哪个catalog。若没有设置catalog，则默认使用HiveCatalog
   2. 在使用HadoopCatalog时，必须设置`set iceberg.catalog.<catalog_name>.warehouse=hdfs://bigdata01:9000/iceberg`指定warehouse路径，在建表时，也必须设置location参数为‘${iceberg.catalog.<catalog_name>.warehouse}/库名/表名’这样的结构
   3. 当表参数中设置`tblproperties('iceberg.catalog' = 'location_based_table')`，表示将iceberg数据对应的路径加载成iceberg表，目录下的结构应该是iceberg表的结构；用于当Spark、Flink写数据到HDFS上，我们想把数据映射成一张Iceberg表的时候使用
6. 由于Hive建表语句分区语法“partition by”的限制， 如果使用Hive创建Iceberg表，目前只能按照Hive语法来写，底层转换成iceberg标示分区，这种情况下不能使用Iceberg的分区转换，例如：days(timestamp)，如果想要使用Iceberg格式表的分区转换标识分区，需要使用Spark或者Flink创建表

7. 如果加载iceberg数据路径是分区表，创建iceberg表时，分区字段当作普通字段处理就可以


### 七、Spark和Iceberg整合

4. hive想访问hadoop_catalog创建的表，则在hive中建表时，应指定tblproperties('iceberg.catalog'='location_based_table')