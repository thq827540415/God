package com.ivi.bigdata.common.rpc.hadoop.protobuf.client;

import com.ivi.bigdata.common.rpc.hadoop.protobuf.proto.MyResourceTrackerMessage;
import com.google.protobuf.ServiceException;
import org.apache.hadoop.conf.Configuration;

public class ProtobufRpcClient {

    static class RpcClient {

        // ClientSideProxy
        private final MyResourceTracker client;

        public RpcClient(Configuration conf) throws Exception {
            client = MyProxyFactory.createProxy(conf, MyResourceTracker.class);
        }

        /**
         * 构建请求对象
         */
        MyResourceTrackerMessage.MyRegisterNodeManagerResponseProto registerNodeManager(
                String hostname,
                int cpu,
                int memory
        ) throws ServiceException {

            MyResourceTrackerMessage.MyRegisterNodeManagerRequestProto request =
                    MyResourceTrackerMessage.MyRegisterNodeManagerRequestProto
                            .newBuilder()
                            .setHostname(hostname)
                            .setCpu(cpu)
                            .setMemory(memory)
                            .build();

            return client.registerNodeManager(request);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        RpcClient client1 = new RpcClient(conf);
        RpcClient client2 = new RpcClient(conf);

        // 调用协议方法，向服务端发送RPC请求
        MyResourceTrackerMessage.MyRegisterNodeManagerResponseProto response1 =
                client1.registerNodeManager("node01", 64, 128);

        MyResourceTrackerMessage.MyRegisterNodeManagerResponseProto response2 =
                client2.registerNodeManager("node02", 322, 64);

        System.out.format("响应结果: node01 -> %s, node02 -> %s\n", response1.getFlag(), response2.getFlag());
    }
}
