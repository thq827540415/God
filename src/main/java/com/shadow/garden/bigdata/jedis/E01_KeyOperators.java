package com.shadow.garden.bigdata.jedis;

import com.solitude.util.BasicEnvUtils;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

@Slf4j
public class E01_KeyOperators {
    /**
     * 从JedisPool中获取jedis连接
     */
    private static final Jedis jedis = BasicEnvUtils.getJedisPoolInstance().getResource();

    public static void main(String[] args) {
        log.info("{}", jedis);
    }

    /**
     * exists key [key...] 判断指定key是否存在，可以传入多个参数时，返回存在的数量
     * dump key 序列化指定key，并返回被序列化的值
     * restore key ttl value 反序列化值到key上，并返回反序列化状态
     */
    private static void dumpAndRestore() {
        // serialized-key首先得存在
        if (!jedis.exists("serialized-key")) {
            String status = jedis.set("serialized-key", "serialized-key");
            log.info("key: serialized-key不存在，创建是否成功: {}", status);
        }
        byte[] serializedKey = jedis.dump("serialized-key");
        log.info("序列化后的值为: {}",  serializedKey);

        // deserialized-key首先得不存在
        if (jedis.exists("deserialized-key")) {
            Long delNum = jedis.del("deserialized-key");
            log.info("key: deserialized-key已存在，删除是否成功: {}", delNum == 1);
        }
        // ttl >= 0,
        String status = jedis.restore("deserialized-key", 0, serializedKey);
        log.info("反序列化是否成功: {}，值为: {}", status, jedis.get("deserialized-key"));
    }

    /**
     * expire key seconds       给key设定过期时间，单位s                        当key不存在时，返回0
     * pexpire key milliseconds 给key设定过期日期，单位ms
     * <p>
     * expireat key timestamp   指定key在某个timestamp过期，timestamp的单位为s
     * pexpireat key timestamp  指定key在某个timestamp过期，timestamp的单位为ms
     * <p>
     * ttl key 查看key剩下存活时间，单位s     当key不存在，返回-2；当key永久存活，返回-1
     * pttl key 查看key剩下存活时间，单位ms
     * <p>
     * persist key 让key永久存活 当key不存在时，返回0
     *
     * rename key new-key 重命名key                                 当key不存在时，报错
     * renamenx key new-key 只有当new-key不存在时，将key改名为new-key
     */
    private static void expire() {
    }

    /**
     * object subcommand [arguments [arguments]]
     *        子命令有：（1）refcount 返回给定key引用所存储的值的次数
     *                （2）encoding 返回给定key
     *                （3）idletime 返回给定key自存储以来的空转时间
     */
    private static void object() {
    }

    /**
     * scan cursor [MATCH pattern] [COUNT count] 用于增量迭代一集合元素
     *      （1）scan用于迭代当前数据库中的数据库键
     *      （2）sscan用于迭代集合键中的元素
     *      （3）hscan用于迭代哈希键中的键值对
     *      （4）zscan用于迭代有序结合中的元素，包括元素成员和元素分值
     */
    private static void scan() {

    }

}
