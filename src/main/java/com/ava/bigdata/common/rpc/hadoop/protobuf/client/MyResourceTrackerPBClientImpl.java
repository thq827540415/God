package com.ava.bigdata.common.rpc.hadoop.protobuf.client;

import com.ava.bigdata.common.rpc.hadoop.protobuf.MyResourceTrackerPB;
import com.ava.bigdata.common.rpc.hadoop.protobuf.proto.MyResourceTrackerMessage;
import com.google.protobuf.ServiceException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.ProtobufRpcEngine;
import org.apache.hadoop.ipc.RPC;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @Author lancer
 * @Date 2023/2/22 17:34
 * @Description 等价于 --> ApplicationClientProtocolPBClientImpl
 */
public class MyResourceTrackerPBClientImpl implements MyResourceTracker {

    private final MyResourceTrackerPB rpcProxy;

    public MyResourceTrackerPBClientImpl(Configuration conf, InetSocketAddress addr) throws IOException {
        // ProtobufRpcEngine和WritableRpcEngine，如果不设置，则为WritableRpcEngine
        RPC.setProtocolEngine(conf, MyResourceTrackerPB.class, ProtobufRpcEngine.class);

        // 获取代理对象，其中包含了一个org.apache.hadoop.ipc.Client客户端
        this.rpcProxy = RPC.getProxy(
                // 与服务端通信的协议接口
                MyResourceTrackerPB.class,
                1,
                // 指定服务端的地址
                addr,
                conf);
    }

    @Override
    public MyResourceTrackerMessage.MyRegisterNodeManagerResponseProto registerNodeManager(
            MyResourceTrackerMessage.MyRegisterNodeManagerRequestProto request) throws ServiceException {
        return rpcProxy.registerNodeManager(null, request);
    }
}
