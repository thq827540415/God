topic是逻辑概念，partition是物理概念

生产者/消费者只与leader副本进行交互，而follower副本只负责消息的同步

分区中的所有副本统称AR（assigned Replicas）。

同步超时时间：replica.lag.time.max.ms=10000
ISR: in sync replicas同步副本（包括leader），ISR集合是AR集合中的一个子集
OSR：out of sync replicas同步超时的副本，该副本上次请求leader同步数据距现在的时间间隔超过设置阈值

AR = ISR + OSR


副本手动分配，在创建topic时，不可与--partitions连用
--replica-assignment broker_id_for_part1_replica1:broker_id_for_part1_replica2,
                     broker_id_for_part2_replica1:broker_id_for_part2_replica2, ...


删除topic，server.properties中需要一个参数处于启用状态： delete.topic.enable=true
使用kafka-topics.sh脚本删除主题的行为本质上只是在ZooKeeper中的/admin/delete_topics路径下建一个与待删除主题同名的节点，以标记该topic为待删除的状态。
然后由Kafka控制器异步完成删除。


Kafka只支持增加分区，不支持减少分区