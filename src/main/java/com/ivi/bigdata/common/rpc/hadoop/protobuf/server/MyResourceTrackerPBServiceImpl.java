package com.ivi.bigdata.common.rpc.hadoop.protobuf.server;

import com.ivi.bigdata.common.rpc.hadoop.protobuf.MyResourceTrackerPB;
import com.ivi.bigdata.common.rpc.hadoop.protobuf.client.MyResourceTracker;
import com.ivi.bigdata.common.rpc.hadoop.protobuf.proto.MyResourceTrackerMessage;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;

/**
 * @Author lancer
 * @Date 2023/2/22 18:12
 * @Description --> ApplicationClientProtocolPBServiceImpl
 */
public class MyResourceTrackerPBServiceImpl implements MyResourceTrackerPB {
    private final MyResourceTracker server;

    public MyResourceTrackerPBServiceImpl(MyResourceTracker server) {
        this.server = server;
    }

    @Override
    public MyResourceTrackerMessage.MyRegisterNodeManagerResponseProto registerNodeManager(
            RpcController controller,
            MyResourceTrackerMessage.MyRegisterNodeManagerRequestProto request) throws ServiceException {
        return server.registerNodeManager(request);
    }

    //                             MyResourceTrackerTranslatorPB
    // client   <->   客户端协议(MyResourceTracker)    <->    MyResourceTrackerPB
    //                                                            ^
    //                                                            |
    //                                                            | 获取到RPC调用信息
    //                                                            |
    //                                                            v
    // 通过客户端协议（MyResourceTracker）找到对应的实现    <->     Server（有着每种协议的具体实现）
    //                          MyResourceTrackerServerSideTranslatorPB -> 对具体实现进行封装的类
}
