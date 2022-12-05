package com.shadow.garden.consts;

import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class Consts {
    private Consts(){
    }

    // ======================================== Usual ========================================
    public static final String NC_HOST = "localhost";

    public static final String HDFS_URL = "hdfs://bigdata01:9000";


    // ======================================== MySQL ========================================
    public static final String MYSQL_HOST = "bigdata03";

    public static final int MYSQL_PORT = 3306;

    private static final String MYSQL_DATABASE = "test";

    /**
     * 需要修改数据库
     */
    public static final String MYSQL_URL =
            "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/" + MYSQL_DATABASE
                    + "?useSSL=false&useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true";
    public static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver";
    public static final String MYSQL_USERNAME = "root";
    public static final String MYSQL_PASSWORD = "123456";


    // ========================================== ZK ==========================================
    public static final String ZK_CONN_STR = "bigdata01:2181,bigdata02:2181,bigdata03:2181";


    // ========================================= Kafka =========================================
    public static final String KAFKA_BOOTSTRAP_SERVERS_OR_BROKER_LIST =
            "bigdata01:9092,bigdata02:9092,bigdata03:9092";

    public static final String KAFKA_DEFAULT_KEY_SERIALIZER_CLASS = IntegerSerializer.class.getName();

    public static final String KAFKA_DEFAULT_VALUE_SERIALIZER_CLASS = StringSerializer.class.getName();


    // ======================================== Redis ========================================
    public static final String REDIS_HOST = "localhost";

    public static final int REDIS_PORT = 6379;

    public static final String REDIS_CONN_STR = "redis://" + REDIS_HOST + ":" + REDIS_PORT + "/0";


    // ========================================= Doris =========================================
    public static final String DORIS_FE_STR = "bigdata03:8030";
    public static final String DORIS_USERNAME = "root";
    public static final String DORIS_PASSWORD = "123456";


    // ========================================= Flink =========================================
    /**
     * 设置全局算子的默认并行度 <= maxParallelism
     */
    public static final int FLINK_GLOBAL_OPERATOR_PARALLELISM = 1;

    /**
     * 单个算子的最大并行度
     * the number of key groups，如果job恢复前后maxParallelism不一致，则不能恢复
     * <p>
     * KeyGroupRangeAssignment#computeDefaultMaxParallelism(int) -> default value：127 ~ 32768
     */
    public static final int FLIN_GLOBAL_MAX_PARALLELISM = 4;

    public static final boolean FLINK_WITH_WEB_UI = true;
}
