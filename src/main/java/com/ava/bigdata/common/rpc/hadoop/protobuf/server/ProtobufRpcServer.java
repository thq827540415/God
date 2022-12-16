package com.ava.bigdata.common.rpc.hadoop.protobuf.server;

import com.ava.bigdata.common.rpc.hadoop.protobuf.MyResourceTrackerPB;
import com.ava.bigdata.common.rpc.hadoop.protobuf.MyResourceTrackerPBImpl;
import com.ava.bigdata.common.rpc.hadoop.protobuf.proto.MyResourceTracker;
import com.ava.bigdata.common.rpc.hadoop.protobuf.proto.MyResourceTrackerMessage;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.ProtobufRpcEngine;
import org.apache.hadoop.ipc.RPC;

import java.io.IOException;

public class ProtobufRpcServer {
    public static void main(String[] args) {
        Configuration conf = new Configuration();
        RPC.setProtocolEngine(conf, MyResourceTrackerPB.class, ProtobufRpcEngine.class);

        try {
            new RPC.Builder(conf)
                    .setProtocol(MyResourceTrackerPB.class)
                    .setInstance(
                            MyResourceTracker
                                    .MyResourceTrackerService
                                    .newReflectiveBlockingService(
                                            new MyResourceTrackerPBImpl(new MyResourceTrackerImpl())))
                    .setBindAddress("localhost")
                    .setPort(9999)
                    .setNumHandlers(1)
                    .setVerbose(true)
                    .build()
                    .start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class MyResourceTrackerImpl implements com.ava.bigdata.common.rpc.hadoop.protobuf.MyResourceTracker {
        @Override
        public MyResourceTrackerMessage.MyRegisterNodeManagerResponseProto registerNodeManager(
                MyResourceTrackerMessage.MyRegisterNodeManagerRequestProto request) {
            System.out.println("NodeManager注册消息为：");
            System.out.println(request.getHostname());
            System.out.println(request.getCpu());
            System.out.println(request.getMemory());

            return MyResourceTrackerMessage.MyRegisterNodeManagerResponseProto
                    .newBuilder()
                    .setFlag("true")
                    .build();
        }
    }
}
