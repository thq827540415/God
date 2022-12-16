package com.ava.bigdata.common.rpc.hadoop.protobuf;

import com.ava.bigdata.common.rpc.hadoop.protobuf.proto.MyResourceTrackerMessage;

/**
 * 协议接口，定义服务的方法，似乎也可以在.proto文件中定义
 */
public interface MyResourceTracker {
    MyResourceTrackerMessage.MyRegisterNodeManagerResponseProto registerNodeManager(
            MyResourceTrackerMessage.MyRegisterNodeManagerRequestProto request);
}
