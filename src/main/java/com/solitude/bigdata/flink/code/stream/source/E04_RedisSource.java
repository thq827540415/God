package com.solitude.bigdata.flink.code.stream.source;

import com.solitude.consts.Consts;
import com.solitude.util.FlinkEnvUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.redis.client.*;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @Author lancer
 * @Date 2022/6/16 00:37
 * @Description 使用Async I/O从Redis中读数据，采用Redis异步客户端Vertx
 */
public class E04_RedisSource {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        DataStreamSource<String> source = env.socketTextStream(Consts.NC_HOST, 9999);

        AsyncDataStream
                .unorderedWait(
                        source,
                        new VertxRedis(),
                        3000,
                        TimeUnit.MILLISECONDS,
                        20)
                .print().setParallelism(1);

        env.execute(E04_RedisSource.class.getSimpleName());
    }

    private static class VertxRedis extends RichAsyncFunction<String, String> {

        private transient RedisAPI redis;
        private transient Jedis jedis;

        @Override
        public void open(Configuration parameters) throws Exception {
            // 使用vertx
            RedisOptions redisOptions = new RedisOptions()
                    .setConnectionString(Consts.REDIS_CONN_STR)
                    .setType(RedisClientType.STANDALONE);

            Redis client = Redis.createClient(Vertx.vertx(), redisOptions);
            redis = RedisAPI.api(client);

            // 使用jedis客户端
            jedis = new Jedis(Consts.REDIS_HOST, Consts.REDIS_PORT);
            jedis.select(0);
        }

        @Override
        public void close() throws Exception {
            redis.close();
        }

        @Override
        public void asyncInvoke(String s, ResultFuture<String> resultFuture) throws Exception {
            redis.get(
                    s,
                    new Handler<AsyncResult<Response>>() {
                        @Override
                        public void handle(AsyncResult<Response> responseAsyncResult) {
                            if (responseAsyncResult.succeeded()) {
                                Response result = responseAsyncResult.result();
                                resultFuture.complete(Collections.singletonList(result.toString()));
                            }
                        }
                    });

/*
            CompletableFuture.<String>supplyAsync(new Supplier<String>() {
                @Override
                public String get() {
                    return jedis.get(s);
                }
            }).thenAccept(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    resultFuture.complete(Collections.singletonList(s));
                }
            });*/
        }
    }
}
