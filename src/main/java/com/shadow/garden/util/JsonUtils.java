package com.shadow.garden.util;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.sql.Timestamp;
import java.util.List;

public class JsonUtils {

    private JsonUtils() {
    }

    private static final ObjectMapper om = new ObjectMapper();

    static {
        // om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    static class Person<T, V> {
        public T name;
        public V age;
        // json串中对应的字段名
        @JsonProperty(value = "produce_time")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        public Timestamp produceTime;
    }

    public static void main(String[] args) {

        ObjectNode objectNode = om.createObjectNode();
        ObjectNode nodes = objectNode.putNull("name");

        nodes
                .with("hobby")
                .put("computer", 1)
                .put("phone", 1)
                .putArray("fruit")
                .addObject()
                .put("age", 18)
                .put("gender", "girl");

        System.out.println(nodes.toPrettyString());


        Person<String, List<Integer>> x = parseObject(
                "{\"name\" : \"zs\", \"age\" : [18, 19], \"produce_time\" : \"2022-11-01 14:44:00\"}",
                new TypeReference<Person<String, List<Integer>>>() {
                });
        System.out.println(x);
    }

    /**
     * 当参数带泛型时使用
     */
    public static <T> T parseObject(String record, TypeReference<T> tr) {
        try {
            return om.readValue(record, tr);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * POJO类型使用
     */
    public static <T> T parseObject(String record, Class<T> clazz) {
        try {
            return om.readValue(record, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode parseObject(String record) {
        try {
            return om.readTree(record);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * writeValue() 序列化
     */
    public static String toJson(Object bean, boolean pretty) {
        try {
            ObjectWriter writer = om.writer();
            if (pretty) {
                writer = writer.withDefaultPrettyPrinter();
            }
            return writer.writeValueAsString(bean);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * toPrettyString方法，会将json串转化成树形格式
     * <p>
     * 使用with、putObject、putArray会重新生成引用对象
     * <p>
     * JsonNode是其父类，只用于获取数据，不可改变
     */
    public static ObjectNode getObjectNode() {
        return om.createObjectNode();
    }
}
