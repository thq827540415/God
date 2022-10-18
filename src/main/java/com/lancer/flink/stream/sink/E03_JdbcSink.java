package com.lancer.flink.stream.sink;

import com.lancer.consts.MySQLConsts;
import com.lancer.FlinkEnvUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.jdbc.JdbcStatementBuilder;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * @Author lancer
 * @Date 2022/6/15 20:31
 * @Description 往MySQL中写数据
 */
public class E03_JdbcSink {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        env
                .fromElements(
                        new Person("zs", 18),
                        new Person("ls", 15)
                )
                .addSink(getJdbcSink());

        env.execute(E03_JdbcSink.class.getSimpleName());
    }

    @Data
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Person {
        private String name;
        private int age;
    }

    /**
     * 底层采用传统的JDBC方式写入数据executeBatch()
     */
    private static SinkFunction<Person> getJdbcSink() {
        return JdbcSink.sink(
                // 指定需要执行的语句 -- insert or update
                "insert into person(name, age) values(?, ?)",
                // 使用preparedStatement设定参数
                new JdbcStatementBuilder<Person>() {
                    @Override
                    public void accept(PreparedStatement preparedStatement, Person person) throws SQLException {
                        preparedStatement.setString(1, person.getName());
                        preparedStatement.setInt(2, person.getAge());
                    }
                },
                // JDBC execution options
                JdbcExecutionOptions.builder()
                        // optional: default = 0, meaning no time-based execution is done
                        .withBatchSize(1000)
                        // optional: default = 5000 values
                        .withBatchIntervalMs(200)
                        // optional: default = 3
                        .withMaxRetries(5)
                        .build(),
                // JDBC connection parameters
                new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                        .withUrl(MySQLConsts.MYSQL_URL)
                        .withDriverName(MySQLConsts.MYSQL_DRIVER)
                        .withUsername(MySQLConsts.MYSQL_USERNAME)
                        .withPassword(MySQLConsts.MYSQL_PASSWORD)
                        .build()
        );
    }
}
