package com.ava.bigdata.common.rpc.protobuf.client;

import com.ava.bigdata.common.rpc.protobuf.MyResourceTrackerPB;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.ProtobufRpcEngine;
import org.apache.hadoop.ipc.RPC;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ProtobufRpcClient {
    public static void main(String[] args) throws IOException {

        Configuration conf = new Configuration();

        RPC.setProtocolEngine(conf, MyResourceTrackerPB.class, ProtobufRpcEngine.class);

        // 获取代理
        MyResourceTrackerPB protocolProxy = RPC.getProxy(
                MyResourceTrackerPB.class,
                1,
                new InetSocketAddress("localhost", 9999),
                conf);

        // 构建请求对象
    }
}
