package com.ava.bigdata.common.rpc.hadoop.protobuf.client;

import org.apache.hadoop.conf.Configuration;

import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;

/**
 * @Author lancer
 * @Date 2023/3/2 18:13
 * @Description
 */
public class MyProxyFactory {
    static <T> T createProxy(final Configuration conf,
                          final Class<T> protocol) throws Exception {
        Class<?> clientImplClazz = conf.getClassByName(
                protocol.getPackage().getName() + "." + protocol.getSimpleName() + "PBClientImpl");
        Constructor<?> constructor = clientImplClazz.getConstructor(Configuration.class, InetSocketAddress.class);
        constructor.setAccessible(true);

        return (T) constructor.newInstance(conf, new InetSocketAddress("localhost", 9999));
    }
}
