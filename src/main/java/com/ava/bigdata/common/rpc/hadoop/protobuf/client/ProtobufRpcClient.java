package com.ava.bigdata.common.rpc.hadoop.protobuf.client;

import com.ava.bigdata.common.rpc.hadoop.protobuf.MyResourceTrackerPB;
import com.ava.bigdata.common.rpc.hadoop.protobuf.proto.MyResourceTrackerMessage;
import com.google.protobuf.ServiceException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.ProtobufRpcEngine;
import org.apache.hadoop.ipc.RPC;

import java.io.IOException;
import java.net.InetSocketAddress;

public class ProtobufRpcClient {
    public static void main(String[] args) {
        Configuration conf = new Configuration();

        // ProtobufRpcEngine和WritableRpcEngine，如果不设置，默认为WritableRpcEngine
        RPC.setProtocolEngine(conf, MyResourceTrackerPB.class, ProtobufRpcEngine.class);

        try {
            // 获取代理对象，其中包含了一个org.apache.hadoop.ipc.Client客户端
            MyResourceTrackerPB protocolProxy = RPC.getProxy(
                    MyResourceTrackerPB.class,
                    1,
                    new InetSocketAddress("localhost", 9999),
                    conf);

            // 构建请求对象
            MyResourceTrackerMessage.MyRegisterNodeManagerRequestProto request =
                    MyResourceTrackerMessage.MyRegisterNodeManagerRequestProto
                            .newBuilder()
                            .setHostname("bigdata02")
                            .setCpu(64)
                            .setMemory(128)
                            .build();

            MyResourceTrackerMessage.MyRegisterNodeManagerResponseProto response =
                    protocolProxy.registerNodeManager(null, request);

            System.out.println("响应结果" + response.getFlag());
        } catch (IOException | ServiceException e) {
            e.printStackTrace();
        }
    }
}
