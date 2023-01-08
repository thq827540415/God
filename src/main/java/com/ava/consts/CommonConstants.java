package com.ava.consts;

import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class CommonConstants {
    // ======================================== Usual ========================================
    public static final String NC_HOST = "localhost";

    public static final String HDFS_URL = "hdfs://bigdata01:9000";

    // ========================================== ZK ==========================================
    public static final String ZK_CONN_STR = "bigdata01:2181,bigdata02:2181,bigdata03:2181";

    // ========================================= FlinkKafka =========================================
    public static final String KAFKA_BOOTSTRAP_SERVERS_OR_BROKER_LIST =
            "bigdata01:9092,bigdata02:9092,bigdata03:9092";

    public static final String KAFKA_DEFAULT_KEY_SERIALIZER_CLASS = IntegerSerializer.class.getName();

    public static final String KAFKA_DEFAULT_VALUE_SERIALIZER_CLASS = StringSerializer.class.getName();
}
