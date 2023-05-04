package com.ivi.code.java;

import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import scala.Tuple2;

import java.util.Arrays;
import java.util.List;

/**
 * @author lancer
 * @date 2022/7/27 23:35
 * @description
 */
@Slf4j
public class WordCountWithJava {
    public static void main(String[] args) {
        SparkConf conf = new SparkConf()
                .setAppName("WordCountWithJava")
                .setMaster("local");

        JavaSparkContext javaSparkContext = new JavaSparkContext(conf);

        List<Tuple2<String, Integer>> result = javaSparkContext.parallelize(Arrays.asList("hello world", "hello spark"))
                .flatMap(s -> Arrays.stream(s.split(" ")).iterator())
                .mapToPair(s -> Tuple2.apply(s, 1))
                .reduceByKey(Integer::sum)
                .collect();

        result.forEach(System.out::println);

        javaSparkContext.close();
    }
}