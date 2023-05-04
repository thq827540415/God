package com.ivi.bigdata.common.rpc.akka;

import com.ivi.bigdata.common.rpc.akka.framework.client.AkkaRpcClientProvider;

public class ClientSide {
    public static void main(String[] args) {
        AkkaRpcClientProvider<DemoService> clientProvider = new AkkaRpcClientProvider<>();
        clientProvider
                .setAddress("akka.tcp://rpcSys@localhost:10086/user/akkaRpcServer")
                .setInterfaceClass(DemoService.class);

        DemoService demoService = clientProvider.get();
        String result = demoService.sayHello("akka");
        System.out.println(result);
    }
}