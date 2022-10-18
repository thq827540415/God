package com.lancer.flink.stream.sink;

import com.lancer.FlinkEnvUtils;
import com.lancer.consts.RedisConsts;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.redis.RedisSink;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper;

/**
 * @Author lancer
 * @Date 2022/6/16 00:16
 * @Description 将数据写入Redis
 */
public class E04_RedisSink {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        SingleOutputStreamOperator<Tuple2<String, String>> line2Tuple = env
                .socketTextStream("localhost", 9999)
                .map(line -> {
                    String[] split = line.split(",");
                    return Tuple2.of(split[0], split[1]);
                })
                .returns(TypeInformation.of(new TypeHint<Tuple2<String, String>>() {
                }));

        FlinkJedisPoolConfig redisConf = new FlinkJedisPoolConfig.Builder()
                .setHost(RedisConsts.REDIS_HOST)
                .setPort(RedisConsts.REDIS_PORT)
                .setDatabase(0)
                .build();

        line2Tuple.addSink(new RedisSink<>(redisConf, new RedisExampleMapper()));

        env.execute(E04_RedisSink.class.getSimpleName());
    }

    // 单机模式的redis
    private static class RedisExampleMapper implements RedisMapper<Tuple2<String, String>> {

        @Override
        public RedisCommandDescription getCommandDescription() {
            // additional key for Hash and Sorted set data type
            // hset word key value
            return new RedisCommandDescription(RedisCommand.HSET, "word");
        }

        @Override
        public String getKeyFromData(Tuple2<String, String> data) {
            return data.f0;
        }

        @Override
        public String getValueFromData(Tuple2<String, String> data) {
            return data.f1;
        }
    }
}
