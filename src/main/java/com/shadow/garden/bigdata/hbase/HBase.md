## 一、数据视图

### 逻辑模型

1. table（表）：一张表包含多行数据。
2. row（行）：
   - 一行数据包含一个唯一标识rowkey、多个column以及对应的值。
   - 一张表中所有的row都按照rowkey的字典序由小到大排序。
3. column（列）：
   - 由column family（列簇）以及qualifier（列名）两部分组成。
   - 列簇在表创建的时候需要指定，用户不能随意增减。
   - 一个列簇下可以设置多个qualifier。
   - HBase会把相同列簇的列尽量放在同一台机器上。
   - 表的很多属性，比如过期时间、数据块缓存以及是否压缩等都是定义在列簇上。
   - 列簇越少越好，太多会极大程度地降低数据库性能。
4. cell（单元格）：
   - 由五元组（row，column，timestamp，type，value）组成的结构。
   - 其中type标识Put/Delete这样的操作类型。
   - timestamp表示这个cell的版本。
   - （row，column，timestamp，type）是K，value是V。
5. timestamp（时间戳）：
   - 每个cell在写入HBase的时候都会默认分配一个时间戳作为该cell的版本。
   - 用户在写入数据的时候可以自带时间戳。
   - 同一rowkey或column下可以有多个value存在，这些value使用timestamp作为版本号，版本越大，数据越新。

### 多维稀疏排序Map

1. 多维：key是一个复合数据结构，由多维元素组成。
2. 稀疏：对于HBase，空值不需要任何填充。如果使用填充null的策略，势必会造成大量空间的浪费。
3. 排序：如果rowkey相同，再比较column，依次往下比较。多维元素排序规则对于提升HBase的读性能至关重要。
4. 分布式：构成HBase的所有Map并不集中在某台机器上，而是分布在整个集群中。

### 物理视图

1. HBase中的数据是按照列簇存储的，即将数据按照列簇分别存储在不同的目录中。

2. 列簇anchor的所有数据存储在一起形成：

   ![](./images/列簇anchor物理视图.png)

3. 列簇contents的所有数据存储在一起形成：

   ![](./images/列簇contents物理视图.png)

## 二、体系结构

