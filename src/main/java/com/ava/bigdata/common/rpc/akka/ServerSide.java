package com.ava.bigdata.common.rpc.akka;

import akka.actor.ActorRef;
import com.ava.bigdata.common.rpc.akka.framework.server.AkkaRpcServerProvider;

public class ServerSide {
    public static void main(String[] args) {
        DemoServiceImpl demoService = new DemoServiceImpl();
        AkkaRpcServerProvider<DemoService> serverProvider = new AkkaRpcServerProvider<>();
        serverProvider
                .setName("akkaRpcServer")
                .setPort(10086)
                .setRef(demoService)
                .setInterfaceClass(DemoService.class);
        ActorRef actorRef = serverProvider.get();
        System.out.println(actorRef.path());
    }
}
