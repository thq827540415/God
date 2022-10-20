package com.lancer.flink.stream.source;

import com.lancer.consts.MySQLConsts;
import com.lancer.FlinkEnvUtils;
import com.lancer.consts.UsualConsts;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @Author lancer
 * @Date 2022/6/15 21:58
 * @Description 使用Async I/O从MySQL中读数据，采用JDBC异步客户端Vertx
 */
public class E03_JdbcSource {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        DataStreamSource<String> source = env.socketTextStream(UsualConsts.NC_HOST, 9999);

        // 响应结果的顺序和请求的先后顺序不一致
        AsyncDataStream
                .unorderedWait(
                        source,
                        new VertxJdbc(),
                        3000,
                        TimeUnit.MILLISECONDS,
                        10)
                .filter(t -> StringUtils.isNotBlank(t.f0))
                .print().setParallelism(1);

        env.execute(E03_JdbcSource.class.getSimpleName());
    }

    /**
     * 采用异步框架读取数据，还可以采用线程池 + 连接池
     */
    private static class VertxJdbc extends RichAsyncFunction<String, Tuple2<String, Integer>> {

        private transient JDBCPool pool;

        /**
         * 配置异步客户端
         */
        @Override
        public void open(Configuration parameters) throws Exception {
            JsonObject config = new JsonObject()
                    .put("url", MySQLConsts.MYSQL_URL)
                    .put("driver_class", MySQLConsts.MYSQL_DRIVER)
                    .put("user", MySQLConsts.MYSQL_USERNAME)
                    .put("password", MySQLConsts.MYSQL_PASSWORD)
                    .put("max_pool_size", 10);

            pool = JDBCPool.pool(Vertx.vertx(), config);
        }

        @Override
        public void close() throws Exception {
            pool.close();
        }

        @Override
        public void asyncInvoke(String s, ResultFuture<Tuple2<String, Integer>> resultFuture) throws Exception {
            pool
                    .preparedQuery("select * from person where name = ?")
                    .execute(Tuple.of(s))
                    .onFailure(
                            new Handler<Throwable>() {
                                @Override
                                public void handle(Throwable throwable) {
                                    // 执行失败打日志
                                    System.out.println("failure");
                                }
                            })
                    .onSuccess(
                            new Handler<RowSet<Row>>() {
                                @Override
                                public void handle(RowSet<Row> rows) {
                                    if (rows.size() != 0) {
                                        for (Row row : rows) {
                                            String name = row.getString(0);
                                            Integer age = row.getInteger("age");
                                            resultFuture.complete(Collections.singletonList(Tuple2.of(name, age)));
                                        }
                                    } else {
                                        resultFuture.complete(Collections.singletonList(Tuple2.of("", -1)));
                                    }
                                }
                            });
        }
    }

}
