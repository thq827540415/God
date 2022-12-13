package com.ava.bigdata.common.rpc.protobuf.server;

import com.ava.bigdata.common.rpc.protobuf.MyResourceTrackerPB;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.ProtobufRpcEngine;
import org.apache.hadoop.ipc.RPC;

public class ProtobufRpcServer {
    public static void main(String[] args) {
        Configuration conf = new Configuration();
        RPC.setProtocolEngine(conf, MyResourceTrackerPB.class, ProtobufRpcEngine.class);
    }
}
