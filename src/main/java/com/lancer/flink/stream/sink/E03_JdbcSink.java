package com.lancer.flink.stream.sink;

import com.lancer.consts.MySQLConsts;
import com.lancer.FlinkEnvUtils;
import com.mysql.cj.jdbc.MysqlXADataSource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.flink.connector.jdbc.*;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.util.function.SerializableSupplier;

import javax.sql.XADataSource;
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
                        new Person("ls", 15))
                // .addSink(getJdbcSink());
                .addSink(getJdbcExactlyOnceSink());

        env.execute(E03_JdbcSink.class.getSimpleName());
    }

    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Person {
        public String name;
        public int age;
    }

    /**
     * 不保证Exactly_Once
     * 底层采用传统的JDBC方式写入数据executeBatch()
     */
    private static SinkFunction<Person> getJdbcSink() {
        return JdbcSink.sink(
                // 指定需要执行的语句 -- insert or update
                // 为保证幂等性，需要创建主键表，再insert into person values(?, ?) duplicated key update name = ?, age = ?
                "insert into person(name, age) values(?, ?)",
                // 使用preparedStatement设定参数
                new JdbcStatementBuilder<Person>() {
                    @Override
                    public void accept(PreparedStatement preparedStatement, Person person) throws SQLException {
                        preparedStatement.setString(1, person.name);
                        preparedStatement.setInt(2, person.age);
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
                        .withDriverName(MySQLConsts.MYSQL_DRIVER)
                        .withUrl(MySQLConsts.MYSQL_URL)
                        .withUsername(MySQLConsts.MYSQL_USERNAME)
                        .withPassword(MySQLConsts.MYSQL_PASSWORD)
                        .build()
        );
    }

    /**
     * 保证Exactly_Once
     *      因为XADataSource是支持分布式事务的接口
     */
    private static SinkFunction<Person> getJdbcExactlyOnceSink() {
        return JdbcSink.exactlyOnceSink(
                "insert into person(name, age) values(?, ?)",
                new JdbcStatementBuilder<Person>() {
                    @Override
                    public void accept(PreparedStatement preparedStatement, Person person) throws SQLException {
                        preparedStatement.setString(1, person.name);
                        preparedStatement.setInt(2, person.age);
                    }
                },
                JdbcExecutionOptions.builder()
                        // optional: default = 0, meaning no time-based execution is done
                        .withBatchSize(1000)
                        // optional: default = 5000 values
                        .withBatchIntervalMs(200)
                        // optional: default = 3
                        .withMaxRetries(5)
                        .build(),
                JdbcExactlyOnceOptions.builder()
                        // MySQL only allow a single XA transaction per connection
                        // MySQL不支持同一个连接上存在并行的多个事务
                        .withTransactionPerConnection(true)
                        .build(),
                new SerializableSupplier<XADataSource>() {
                    @Override
                    public XADataSource get() {
                        // XADataSource是支持分布式事务的连接
                        MysqlXADataSource ds = new MysqlXADataSource();
                        ds.setUrl(MySQLConsts.MYSQL_URL);
                        ds.setUser(MySQLConsts.MYSQL_USERNAME);
                        ds.setPassword(MySQLConsts.MYSQL_PASSWORD);
                        return ds;
                    }
                }
        );
    }
}
