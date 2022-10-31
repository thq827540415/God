package com.lancer.example;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lancer.utils.FlinkEnvUtils;
import com.lancer.utils.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.sql.Timestamp;

public class Example {

    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {
        public String productId;
        public String productStatus;
        @JsonProperty(value = "machine_id")
        public String machineId;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        public Timestamp produceTime;
    }
    public static void main(String[] args) {

        System.out.println(JsonUtils.parseObject(
                "{\"machine_id\": \"1\", \"productId\": \"1\", \"productStatus\": \"finished\", \"produceTime\": \"2022-10-31 14:01:00\"}",
                Product.class));

    }


    private static void doTask01() {

    }

    private static void doTask02() {

    }

    private static void doTask03() {

    }

    private static void doTask04() {

    }

    private static void doTask05() {

    }

    private static void doTask06() {

    }
}
