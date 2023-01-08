package com.ava.bigdata.flink.code.stream.source;

import com.ava.consts.CommonConstants;
import com.ava.util.BasicEnvUtils;
import com.ava.util.FlinkEnvUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @Author lancer
 * @Date 2022/6/16 00:37
 * @Description 使用Async I/O从Redis中读数据，采用Redis异步客户端Vertx
 */
public class RedisSource {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        DataStreamSource<String> source = env.socketTextStream(CommonConstants.NC_HOST, 9999);

        AsyncDataStream
                .unorderedWait(
                        source,
                        new VertxRedis(),
                        3000,
                        TimeUnit.MILLISECONDS,
                        20)
                .print().setParallelism(1);

        env.execute(RedisSource.class.getSimpleName());
    }

    private static class VertxRedis extends RichAsyncFunction<String, String> {

        private transient JedisPool pool;

        @Override
        public void open(Configuration parameters) throws Exception {
            // 使用jedis客户端
            pool = BasicEnvUtils.getJedisPoolInstance();
        }

        @Override
        public void close() throws Exception {
            pool.close();
        }

        @Override
        public void asyncInvoke(String s, ResultFuture<String> resultFuture) throws Exception {
            CompletableFuture
                    .supplyAsync(
                            () -> {
                                Jedis jedis = pool.getResource();
                                String result = jedis.get(s);
                                jedis.close();
                                return result;
                            })
                    .thenAccept(
                            result -> {
                                if (result != null) {
                                    resultFuture.complete(Collections.singletonList(result));
                                }
                            });
        }
    }
}
