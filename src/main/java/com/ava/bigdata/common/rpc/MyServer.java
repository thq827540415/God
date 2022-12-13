package com.ava.bigdata.common.rpc;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;

import java.io.IOException;

public class MyServer {
    public static void main(String[] args) {
        try {
            // 服务端提供了一个BusinessProtocol协议的BusinessImpl服务实现
            RPC.Server server = new RPC.Builder(new Configuration())
                    .setProtocol(BusinessProtocol.class)
                    .setInstance(new BusinessImpl())
                    .setBindAddress("localhost")
                    .setPort(6789)
                    .build();

            // RPC Server启动
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}