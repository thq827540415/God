package com.ivi.code.stream.source;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.ivi.consts.CommonConstants;
import com.ivi.consts.MysqlConstants;
import com.ivi.code.util.FlinkEnvUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @Author lancer
 * @Date 2022/6/15 21:58
 * @Description 使用Async I/O从MySQL中读数据，采用JDBC异步客户端Vertx
 */
public class JdbcSource {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        DataStreamSource<String> source = env.socketTextStream(CommonConstants.NC_HOST, 9999);

        // 响应结果的顺序和请求的先后顺序不一致
        AsyncDataStream
                .unorderedWait(
                        source,
                        new DataSourceJdbc("select name, age from person where name = ?"),
                        3000,
                        TimeUnit.MILLISECONDS,
                        // 异步请求队列最大的数量，不传入该参数，默认值为100
                        10)
                .filter(t -> StringUtils.isNotBlank(t.f0))
                .print().setParallelism(1);

        env.execute(JdbcSource.class.getSimpleName());
    }

    /**
     * 采用线程池 + 连接池
     */
    private static class DataSourceJdbc extends RichAsyncFunction<String, Tuple2<String, String>> {

        private transient DruidDataSource ds;
        private transient ExecutorService executorService;

        private final String sql;

        public DataSourceJdbc(String sql) {
            this.sql = sql;
        }

        @Override
        public void open(Configuration parameters) throws Exception {
            executorService = Executors.newFixedThreadPool(
                    Math.min(getRuntimeContext().getNumberOfParallelSubtasks(), 10),
                    new ThreadFactory() {
                        @Override
                        public Thread newThread(@NotNull Runnable r) {
                            Thread t = new Thread(r);
                            t.setDaemon(true);
                            return t;
                        }
                    });

            ds = new DruidDataSource();
            ds.setDriverClassName(MysqlConstants.DRIVER);
            ds.setUrl(MysqlConstants.URL);
            ds.setUsername(MysqlConstants.USERNAME);
            ds.setPassword(MysqlConstants.PASSWORD);
            ds.setMaxActive(10);
        }

        @Override
        public void asyncInvoke(String input, ResultFuture<Tuple2<String, String>> resultFuture) throws Exception {
            CompletableFuture
                    .supplyAsync(
                            new Supplier<String>() {
                                @Override
                                public String get() {
                                    try {
                                        DruidPooledConnection conn = ds.getConnection();
                                        PreparedStatement ps = conn.prepareStatement(sql);

                                        ps.setString(1, input);
                                        ResultSet rs = ps.executeQuery();
                                        String age = "";
                                        while (rs.next()) {
                                            age = rs.getString("age");
                                        }
                                        rs.close();
                                        ps.close();
                                        conn.close();
                                        return age;
                                    } catch (SQLException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }, executorService)
                    .thenAccept(
                            new Consumer<String>() {
                                @Override
                                public void accept(String s) {
                                    resultFuture.complete(Collections.singleton(Tuple2.of(input, s)));
                                }
                            });
        }

        @Override
        public void close() throws Exception {
            ds.close();
            executorService.shutdown();
        }
    }

}
