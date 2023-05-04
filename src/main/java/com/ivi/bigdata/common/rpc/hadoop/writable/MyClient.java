package com.ivi.bigdata.common.rpc.hadoop.writable;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MyClient {
    public static void main(String[] args) {
        try {
            // 获取了服务端中暴露了的服务协议的一个代理
            // 客户端通过这个代理可以调用服务端的方法进行逻辑处理
            BusinessProtocol proxy = RPC.getProxy(
                    BusinessProtocol.class,
                    // 不同版本的Server和Client之间是不能通信的
                    BusinessProtocol.versionID,
                    new InetSocketAddress("localhost", 6789),
                    new Configuration());

            // 客户端调用服务端的代码执行，真正的代码执行是在服务端
            proxy.mkdir("/test");
            String rpcRes = proxy.getName("bigdata");
            System.out.println("从RPC服务端接收到的getName RPC请求的响应结果为：" + rpcRes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
