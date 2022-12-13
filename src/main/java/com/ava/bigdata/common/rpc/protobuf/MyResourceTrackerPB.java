package com.ava.bigdata.common.rpc.protobuf;

import org.apache.hadoop.ipc.ProtocolInfo;
import com.ava.bigdata.common.rpc.protobuf.proto.MyResourceTracker;

/**
 * 编写proto的协议接口
 */
@ProtocolInfo(protocolName = "com.ava.bigdata.common.rpc.protobuf.MyResourceTrackerPB", protocolVersion = 1)
public interface MyResourceTrackerPB extends MyResourceTracker.MyResourceTrackerService.BlockingInterface {
}
