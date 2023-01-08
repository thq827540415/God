package com.ava.consts;

import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class KafkaConstants {
    public static final String BOOTSTRAP_SERVERS_OR_BROKER_LIST =
            "bigdata01:9092,bigdata02:9092,bigdata03:9092";

    public static final String DEFAULT_KEY_SERIALIZER_CLASS = IntegerSerializer.class.getName();

    public static final String DEFAULT_VALUE_SERIALIZER_CLASS = StringSerializer.class.getName();
}
