package com.ivi.bigdata.common.rpc.akka.framework.server;

import akka.actor.AbstractLoggingActor;
import com.ivi.bigdata.common.rpc.akka.framework.RpcRequest;
import com.ivi.bigdata.common.rpc.akka.framework.RpcResponse;

public class AkkaRpcServerActor<T> extends AbstractLoggingActor {

    private final T ref;

    private final Class<?> interfaceClass;

    public AkkaRpcServerActor(T ref, Class<?> interfaceClass) {
        this.ref = ref;
        this.interfaceClass = interfaceClass;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(RpcRequest.class,
                        request -> {
                            log().info("Received request: {}", request);
                            // 处理请求
                            RpcResponse response = handleRequest(request);
                            // 将结果返回
                            log().info("Send response to client. {}", response);
                            getSender().tell(response, getSelf());
                        })
                .build();
    }

    private RpcResponse handleRequest(RpcRequest request) {
        RpcResponse response = new RpcResponse();
        try {
            log().info("The server is handling request.");
            response.setData(
                    interfaceClass.getMethod(request.getMethodName(), request.getParameterTypes())
                            .invoke(ref, request.getParameters()));
        } catch (Exception e) {
            response.setStatus(RpcResponse.FAILED).setMessage(e.getMessage());
        }
        return response;
    }
}
