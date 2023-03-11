package com.ava.bigdata.common.rpc.hadoop.protobuf;

import com.ava.bigdata.common.rpc.hadoop.protobuf.proto.MyResourceTrackerProtocol.MyResourceTrackerService;
import org.apache.hadoop.ipc.ProtocolInfo;

/**
 * 真正进行通信的协议接口
 */
@ProtocolInfo(
        protocolName = "com.ava.bigdata.common.rpc.hadoop.protobuf.MyResourceTrackerPB",
        protocolVersion = 1)
public interface MyResourceTrackerPB extends
        MyResourceTrackerService.BlockingInterface {
}
