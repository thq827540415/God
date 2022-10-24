flink并不能实现真正意义上的end-to-end的Exactly_Once，也不能做到EOS中每条数据只被处理一次，而是实现了state的精确一致（故障重启后，每个算子的状态都恢复到最新的被统一数据影响后的状态的值）
，end-to-end的EOS是通过Source、算子状态、Sink端的配置，来实现（其中的数据可能被处理了多次，但是最终还是一致的）

Source端：能够记录偏移量在状态、能够重放数据（数据并不是发送过就没了）

flink内部算子状态：checkpoint -> 基于chandy-lamport分布式快照算法，往数据流中插入barrier（Watermark类似），barrier也是一种StreamRecord

    进行checkpoint的时候，必须保证算子的状态被同一数据影响后，才能保存
    对齐的checkpoint：
                两条流连接后，接收两个barrier，只有当两个barrier到齐后，才进行checkpoint，会产生背压，对数据处理效率有影响
    非对齐的checkpoint只能保证at least once
    checkpoint调用流程实质是2PC
    做checkpoint时，JM会向Source的每个subtask发送checkpointid，source接收到ck请求后，通过广播的方式，将barrier发送到下游，同时本地做一次ck，
                当ck完成后，会给JM发送ack，当JM接收到每个节点的ack后，会给每个节点callback，告知该次ck已完成

Sink端：幂等写入、2PC事务提交、2PC预写日志提交、HDFS文件

        （1）HDFS: flink正常写in-gress文件，当发起chckpoint的时候in-gress文件改成pending，
    当checkpoint完成时pending改成finish. 如果checkpoint成功后还没来得及改就挂掉文件依然是pending, 
    当flink任务从checkpoint起来后会得到状态，将pending文件改成finsh，将in-gress文件给清理掉(2.7版本以后支持) 
    这样就保证了一致性
        （2）幂等写入: 目标存储系统支持幂等写入，且数据中有合适的主键（MySQL、HBase、Redis）
                能实现最终一致，但有可能存在过程中的不一致（能保证最终的key只有一个，但是当key对应的值不确定时（随机），可能出现过程不一致）
                如往MySQL表(id primary key, name, age)插入一条数据1, zs, 18, 此时任务失败，重新插入的数据为1, ls, 20，则会导致之前的数据被覆盖掉
                缺点：某种定义上的脏读、以及过程不一致
                
                虽然Kafka Producer到Broker内部支持幂等性，但是作为Flink的Sink，则不支持幂等写入。
                        Kafka Producer往Broker中发送数据时，若Producer没有收到Broker的ack，则会一直发该条数据，
                    但是Broker会将收到的数据与缓存中的数据的producerid和sequence num进行比较，认为只有一条数据，则只会写入一条数据，
                    这是Kafka内部的幂等性。
                        作为Flink Sink时，当任务失败重试时，同一条数据，会被Producer认为是不同的数据，且分配不同的序列号，Broker则会认为
                    该数据是一条新数据，也会将其写入，此时就造成了该数据的重复写入，故在Flink EOS Sink中，Kafka是不支持幂等写入的
        （3）2PC事务提交（Kafka伪事务）
                Kafka中，发送数据时，开启事务会给数据加上一个标记消息（control message），任务失败重试时，事务中的数据被写入到kafka，
                    但是该条数据没有事务结束的标记，在后续的Kafka消费者中，通过设置事务的隔离级别ReadCommited，将读取到的没有事务结束标记的消息给过滤掉，
                    从而实现了Flink EOS