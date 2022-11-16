package com.lumine.bigdata.consts;

import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * @Author lancer
 * @Date 2022/8/4 21:28
 * @Description
 */
public class KafkaConsts {
    public static final String BOOTSTRAP_SERVERS_OR_BROKER_LIST =
            "bigdata01:9092,bigdata02:9092,bigdata03:9092";

    public static final String DEFAULT_KEY_SERIALIZER_CLASS = IntegerSerializer.class.getName();

    public static final String DEFAULT_VALUE_SERIALIZER_CLASS = StringSerializer.class.getName();
}
