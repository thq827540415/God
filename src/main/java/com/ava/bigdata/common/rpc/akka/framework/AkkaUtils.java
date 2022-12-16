package com.ava.bigdata.common.rpc.akka.framework;

import akka.actor.ActorSystem;
import com.typesafe.config.ConfigFactory;

/**
 * 用来创建能够提供远程服务的AkkaSystem
 */
public class AkkaUtils {
    public static ActorSystem createRemoteActorSystem(String name, int port) {
        String config = "akka.actor.provider = \"akka.remote.RemoteActorRefProvider\"\n" +
                "akka.remote.enabled-transports = [\"akka.remote.netty.tcp\"]\n" +
                "akka.remote.netty.tcp.hostname = \"localhost\"\n" +
                "akka.remote.netty.tcp.port = " + port + "";

        return ActorSystem.create(name, ConfigFactory.parseString(config));
    }
}
