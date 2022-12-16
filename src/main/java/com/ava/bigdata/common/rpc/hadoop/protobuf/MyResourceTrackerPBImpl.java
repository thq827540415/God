package com.ava.bigdata.common.rpc.hadoop.protobuf;

import com.ava.bigdata.common.rpc.hadoop.protobuf.proto.MyResourceTrackerMessage.MyRegisterNodeManagerResponseProto;
import com.ava.bigdata.common.rpc.hadoop.protobuf.proto.MyResourceTrackerMessage.MyRegisterNodeManagerRequestProto;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;

/**
 * 具体提供的服务
 */
public class MyResourceTrackerPBImpl implements MyResourceTrackerPB {

    final MyResourceTracker server;

    public MyResourceTrackerPBImpl(MyResourceTracker server) {
        this.server = server;
    }

    @Override
    public MyRegisterNodeManagerResponseProto registerNodeManager(
            RpcController controller, MyRegisterNodeManagerRequestProto request) throws ServiceException {
        return server.registerNodeManager(request);
    }
}
