package com.ivi.code.stream.sink;

import com.ivi.consts.RedisConstants;
import com.ivi.consts.CommonConstants;
import com.ivi.code.util.FlinkEnvUtils;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper;

import java.util.Optional;

/**
 * @Author lancer
 * @Date 2022/6/16 00:16
 * @Description 将数据写入Redis
 */
public class RedisSink {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        SingleOutputStreamOperator<Tuple2<String, String>> line2Tuple = env
                .socketTextStream(CommonConstants.NC_HOST, 9999)
                .map(
                        line -> {
                            String[] split = line.split(",");
                            return Tuple2.of(split[0], split[1]);
                        })
                .returns(
                        TypeInformation.of(new TypeHint<Tuple2<String, String>>() {
                        }));

        FlinkJedisPoolConfig redisConf = new FlinkJedisPoolConfig.Builder()
                .setHost(RedisConstants.HOST)
                .setPort(RedisConstants.PORT)
                .setDatabase(0)
                .build();

        line2Tuple.addSink(new org.apache.flink.streaming.connectors.redis.RedisSink<>(redisConf, new RedisExampleMapper()));

        env.execute(RedisSink.class.getSimpleName());
    }

    // 单机模式的redis
    private static class RedisExampleMapper implements RedisMapper<Tuple2<String, String>> {

        @Override
        public RedisCommandDescription getCommandDescription() {
            // additional key for Hash and Sorted set data type
            // cmd: hset additionalKey key value
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


        /**
         * key想要的是学生的name，field是相应的科目，而value是这个科目对应的成绩。
         * 此时就需要动态修改additionalKey
         */
        @Override
        public Optional<String> getAdditionalKey(Tuple2<String, String> data) {
            return RedisMapper.super.getAdditionalKey(data);
        }

        /**
         * 根据具体数据，这是不同的TTL
         */
        @Override
        public Optional<Integer> getAdditionalTTL(Tuple2<String, String> data) {
            return RedisMapper.super.getAdditionalTTL(data);
        }
    }
}
