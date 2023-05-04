package com.ivi.bigdata.common.rpc.hadoop.protobuf.client;

import com.ivi.bigdata.common.rpc.hadoop.protobuf.proto.MyResourceTrackerMessage;
import com.google.protobuf.ServiceException;

/**
 * 客户端协议接口
 */
public interface MyResourceTracker {
    MyResourceTrackerMessage.MyRegisterNodeManagerResponseProto registerNodeManager(
            MyResourceTrackerMessage.MyRegisterNodeManagerRequestProto request) throws ServiceException;
}
