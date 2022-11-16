package com.lumine.bigdata.jedis.util;

import redis.clients.jedis.JedisPool;

import java.net.URI;
import java.net.URISyntaxException;

public class RedisClientUtil {

    private RedisClientUtil(){
    }

    private static class ConnectionHandler {
        private static final JedisPool jp;

        static {
            try {
                jp = new JedisPool(new URI("redis://localhost:6379/0"));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static JedisPool getConnection() {
        return ConnectionHandler.jp;
    }
}
