package com.ava.bigdata.kafka.code.producer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public class E02_Serializer {

    @Data
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Company {
        private String name;
        private String address;
    }

    public static class MySerializer implements Serializer<Company> {
        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
            Serializer.super.configure(configs, isKey);
        }

        @Override
        public byte[] serialize(String topic, Company data) {
            if (Objects.isNull(data)) {
                return null;
            }
            byte[] name, address;

            if (Objects.nonNull(data.getName())) {
                name = data.getName().getBytes(StandardCharsets.UTF_8);
            } else {
                name = new byte[0];
            }

            if (Objects.nonNull(data.getAddress())) {
                address = data.getAddress().getBytes(StandardCharsets.UTF_8);
            } else {
                address = new byte[0];
            }
            // String占用4字节
            ByteBuffer buffer = ByteBuffer.allocate(4 + name.length + 4 + address.length);
            buffer.putInt(name.length);
            buffer.put(name);
            buffer.putInt(address.length);
            buffer.put(address);
            return buffer.array();
        }

        @Override
        public void close() {
            Serializer.super.close();
        }
    }
}
