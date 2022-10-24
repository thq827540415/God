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

Sink端：幂等写入、两阶段提交事务写入、WAL预写日志、HDFS文件

