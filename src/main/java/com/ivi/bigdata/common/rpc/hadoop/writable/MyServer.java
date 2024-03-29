package com.ivi.bigdata.common.rpc.hadoop.writable;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.RPC;

import java.io.IOException;

public class MyServer {
    public static void main(String[] args) {
        try {
            RPC.Server server = new RPC.Builder(new Configuration())
                    .setProtocol(BusinessProtocol.class)
                    // 该协议对应的服务组件，即对应的具体实现
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