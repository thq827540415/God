package com.ava.bigdata.common.rpc.akka.framework.client;

import com.ava.bigdata.common.rpc.akka.framework.RpcRequest;
import com.ava.bigdata.common.rpc.akka.framework.RpcResponse;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class AkkaRpcInvocationHandler implements InvocationHandler {

    private final AkkaRpcClient client;

    public AkkaRpcInvocationHandler(AkkaRpcClient client) {
        this.client = client;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        RpcRequest rpcRequest = new RpcRequest();

        rpcRequest.setMethodName(method.getName())
                .setParameterTypes(method.getParameterTypes())
                .setParameters(args);

        RpcResponse response = (RpcResponse) client.ask(rpcRequest);

        if (RpcResponse.SUCCESS.equals(response.getStatus())) {
            return response.getData();
        }
        throw new RuntimeException(response.getMessage());
    }
}
