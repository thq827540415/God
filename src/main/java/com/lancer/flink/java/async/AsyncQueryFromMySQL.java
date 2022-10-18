package com.lancer.flink.java.async;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AsyncQueryFromMySQL {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, 5000));

        DataStreamSource<String> source = env.socketTextStream("bigdata01", 9999);
        int capacity = 50;
        SingleOutputStreamOperator<Tuple2<String, String>> result = AsyncDataStream.orderedWait(
                source,
                new MySQLAsyncFunction(capacity),
                3000,
                TimeUnit.MILLISECONDS,
                capacity);

        env.execute();
    }

    private static class MySQLAsyncFunction extends RichAsyncFunction<String, Tuple2<String, String>> {

        private transient DruidDataSource dataSource; // 数据库连接池
        private transient ExecutorService executorService; // 用于提交多个异步请求的线程池

        private final int maxConnTotal;

        public MySQLAsyncFunction(int maxConnTotal) {
            this.maxConnTotal = maxConnTotal;
        }

        @Override
        public void open(Configuration parameters) throws Exception {
            // 创建固定大小的线程池
            executorService = Executors.newFixedThreadPool(maxConnTotal);
            // 创建数据库连接池并指定对应的参数
            dataSource = new DruidDataSource();
            dataSource.setDriverClassName("com.mysql.jdbc.Driver");
            dataSource.setUsername("root");
            dataSource.setPassword("123456");
            dataSource.setUrl("jdbc:mysql://bigdata03:3306/project?characterEncoding=UTF-8");
            dataSource.setMaxActive(maxConnTotal);
        }

        @Override
        public void asyncInvoke(String id, ResultFuture<Tuple2<String, String>> resultFuture) throws Exception {
            // 调用线程的submit方法，将查询请求丢入到线程池中异步执行，返回Future对象
            Future<String> future = executorService.submit(() -> queryFromMySQL(id));

            CompletableFuture.supplyAsync(new Supplier<String>() {
                @Override
                public String get() {
                    try {
                        return future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        return null;
                    }
                }
            }).thenAccept(new Consumer<String>() {
                @Override
                public void accept(String result) {
                    resultFuture.complete(Collections.singleton(Tuple2.of(id, result)));
                }
            });
        }

        private String queryFromMySQL(String param) throws SQLException {
            String sql = "select id, info from t_data where id = ?";
            String result = null;
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            ResultSet rs = null;
            try {
                connection = dataSource.getConnection();
                preparedStatement = connection.prepareStatement(sql);
                preparedStatement.setString(1, param);
                rs = preparedStatement.executeQuery();
                while (rs.next()) {
                    result = rs.getString("info");
                }
            } finally {
                if (rs != null) {
                    rs.close();
                }
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            }
            return result;
        }

        @Override
        public void close() throws Exception {
            dataSource.close();
            executorService.shutdown();
        }
    }
}
