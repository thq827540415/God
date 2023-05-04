package com.ivi.bigdata.common.rpc.akka.framework.server;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.ivi.bigdata.common.rpc.akka.framework.AkkaUtils;

/**
 * 用以创建AkkaServerActor实例，启动Akka服务
 * @param <T>
 */
public class AkkaRpcServerProvider<T> {
    private T ref;
    private int port;
    private String name;
    private Class<T> interfaceClass;

    public AkkaRpcServerProvider<T> setRef(T ref) {
        this.ref = ref;
        return this;
    }


    public AkkaRpcServerProvider<T> setPort(int port) {
        this.port = port;
        return this;
    }

    public AkkaRpcServerProvider<T> setName(String name) {
        this.name = name;
        return this;
    }

    public AkkaRpcServerProvider<T> setInterfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
        return this;
    }

    public ActorRef get() {
        ActorSystem system = AkkaUtils.createRemoteActorSystem("rpcSys", port);
        return system.actorOf(Props.create(AkkaRpcServerActor.class, ref, interfaceClass), name);
    }
}

