package com.lancer.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonUtils {
    private static final ObjectMapper om = new ObjectMapper();

    static {
        // om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * readValue() 反序列化
     */
    public static <T> T parseObject(String record, Class<T> clazz) {
        try {
            return om.readValue(record, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * writeValue() 序列化
     */
    public static <T> T toJson() {
        return null;
    }
}
