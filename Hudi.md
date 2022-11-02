Hudi(Hadoop upsert and delete incremental)

提供了表、事务、upsert、delete、高级索引等

hudi是用来管理数据的，给数据存储提供了一种组织方式。hive是用来分析数据的

特性：
2. 丰富的事务支持，支持在文件存储布局上做更新
3. 基于ACID语义的增量处理：增量ETL处理，分钟级别
4. 自动管理小文件


一、hudi最核心的三大组件：
    1. Timeline
        （1）类似数据库事务日志，undo、redo日志，从而实现事务的ACID
        （2）每张表内部维护了一个Timeline
        （3）在Timeline上每个commit被抽象为一个HoodieInstant，而一个Instant记录了一次commit的行为、时间戳和状态
        （4）Timeline的上的HoodieInstant，包含三个部分
            i. Action：
                * commit -> 表示把一批记录原子性地写入到一张表中，生成一个新的file slice，即新的数据版本
                * delta commit -> 将一批记录原子性地写入MOR表中，其中一些或所有数据都可以写入增量日志（deltaLog file）
                * cleans -> 清除表中不再需要的旧版本文件（旧file slice）。hoodie.cleaner.commits.retained保留几个历史file slice，默认为10
                * compaction -> 将行式文件和列式文件合并成新的列式文件，在后台定时执行
                * rollback -> commit或delta commit执行不成功时回滚数据，删除期间产生的任意文件
                * savepoint（将file group标记为saved，cleans执行时不会删除对应的数据）
            ii. Time -> 20221102172300
            iii. State -> completed、inFlight、requested
    2. File Layouts
        （1）partition <-1:n-> (fileid <-1:1-> file group) <-1:n-> file slice(one base file & multi deltaLog files)
        （2）base file采用读优化的列存格式Apache Parquet；deltaLog file采用写优化的行存格式Apache Avro
        （3）采用了多版本控制MVCC，即一个file group中存在多个版本的file slice。当执行compaction操作时，会合并deltaLog file和base file，
            生成新的file slice。cleans操作会清理掉不用的file slice，释放存储空间。
        （4）文件组织结构
            i. 每个表对应DFS上的一个目录
            ii. 每个表目录下，有一个.hoodie文件夹，用于存储Timeline的元数据信息，其中数据的命名规则为time.action.state（其中completed状态的结尾为空）
            iii. 每个表目录对应多个分区目录，每个分区通过一个分区路径（partition path）来唯一标识。每个分区目录下，有该分区的元数据信息，对应文件夹为.hoodie_partition_metadata
            iv. 每个分区目录下，通过file group的方式来组织，每个file group对应一个唯一的fileid。
            v. 每个file group中包含多个file slice，每个file slice包含一个base file（.parquet），这个文件是在commit/compaction操作的时候生成的，
                同时还生成了几个deltaLog file（*.log*），日志文件中包含了从该base file生成以后执行的insert/update操作
    3. Index
        （1）在每个parquet文件的末尾做索引
        （5）Hudi会通过record key与partition path组成HoodieKey，通过将HoodieKey映射到前面提到的fileid，具体其实是映射到file group/fileid，这就是hudi的索引。
            一旦record的第一个版本被写入文件中，对应的HoodieKey就不会再改变了。
        （6）Hudi的base file在根目录中的.hoodie_partition_metadata去记录了record key组成的bloomfilter，用于在file based index的实现中实现高效率的key contains检测，
            决定数据是insert还是update。

二、hudi的Table & Query Type
    1. CopyOnWrite
        （1）在数据写入的时候，复制一份原来的拷贝，在其基础上添加新数据，生成一个新的持有base file的file slice，数据存储格式为parquet，用户在读取数据时，会扫描所有最新的file slice下的base file
        （2）这种表没有compact instant，因为在写入时就相当于compact了。
    2. MergeOnRead
        （1）使用Parquet和Arvo混合的方式来存储数据。更新时写入到增量文件中，之后通过同步或异步的compaction操作，生成新版本的列式格式文件
        （2）通过参数hoodie.compact.inline来开启是否一个事务完成后，执行compact操作，默认为false。
        （3）通过参数hoodie.compact.inline.max.delta.commits来设置多少次commit后，进行compaction，默认是5次。
        （4）以上两个参数都是针对每个file slice而言

Huid的读写API通过Timeline接口，可以方便的在commits上进行条件筛选，对history和on-going的commit应用各种策略，快速筛选出需要操作的目标commit。






快照读取的是最新的数据、增量读取是读取当前修改后的数据、读取优化是读取base file，compaction = base file + delta log -> new base file


CopyOnWrite：适用于读多写少；当写的时候，会复制一份fileslice，然后往里面写数据，同时以前的那份fileslice支持读。
            用户在snapshot读取的时候，会扫描所有file group中最新的fileslice下的base file。

MergeOnRead：适用于读少写多；当写的时候，会将数据写到avro格式的delta log文件中，然后会定期进行compaction，将delta log和base file合并，是在base file上将delta log中的动作replay一遍。
            在read optimized模式下，只会读取最近经过compaction的commit。在snapshot下，会先将base file和delta log进行merge，然后返回，此时并不是compaction



hudi支持三种方式写入数据：
    （1）upsert默认，数据先通过index打标"（record key + partition -> file group -> fileslice -> bloomfilter判断是insert还是update）"
        1. COW
            1. 先对records按照record key去重；
            2. 首先对这批数据创建索引（HoodieKey -> HoodieRecordLocation）；通过索引区分那些records是update，哪些是insert
            3. 对于update的消息，会直接找到对应key所在的最新fileslice的base file文件， 并做merge后写新的base file，生成新的fileslice
            4. 对于insert的消息，会扫描当前partition的所有smallfile（小于一定大小的base file），然后merge写新的fileslice，如果没有smallfile，
                则直接写新的fileGroup + fileslice
        2. MOR
            i. 先对records按照record key去重；
            ii. 首先对这批数据创建索引（HoodieKey -> HoodieRecordLocation）；通过索引区分那些records是update，哪些是insert
            iii. 如果是update的消息，写对应的fileGroup + fileslice，直接append最新的log file（如果碰巧是当前最小的文件，会merge base file，生成新的fileslice）log file大小达到阈值会roll over一个新的
            vi. 如果是insert的消息，如果log file不可建索引，会尝试merge分区内最小的base file（不包含log file的fileslice），生成新的fileslice；
                如果没有base file就新写一个file group + fileslice + base file；如果log file可建索引，尝试append小的logfile，如果没有就新写一个file group + fileslice + base file。
                insert操作是会新生成一个base file的
    （2）insert，跳过index，写入效率更高
    （3）bulk insert，写排序，对大数据量的hudi表初始化友好，对文件大小的限制best effort（写HFile）