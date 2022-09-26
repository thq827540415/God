package com.lancer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @Author lancer
 * @Date 2022/6/9 00:38
 * @Description
 */
public class TransformationApp {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Access {
        private Long time;
        private String domain;
        private double traffic;
    }
}
