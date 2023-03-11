package com.ava.bigdata.common.rpc.hadoop.protobuf.server;

import com.ava.bigdata.common.rpc.hadoop.protobuf.AllProtocols;
import com.ava.bigdata.common.rpc.hadoop.protobuf.MyResourceTrackerPB;
import com.ava.bigdata.common.rpc.hadoop.protobuf.client.MyResourceTracker;
import com.ava.bigdata.common.rpc.hadoop.protobuf.proto.MyResourceTrackerMessage;
import com.google.protobuf.BlockingService;
import com.google.protobuf.ServiceException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.ProtobufRpcEngine;
import org.apache.hadoop.ipc.RPC;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;

public class ProtobufRpcServer {
    /**
     * 对AllProtocols中所有协议的具体实现
     */
    static class RpcServer implements AllProtocols {
        public RpcServer(Configuration conf) throws Exception {
            RPC.Server server = getServer(conf, MyResourceTracker.class);

            /*RPC.setProtocolEngine(conf, MyResourceTrackerPB.class, ProtobufRpcEngine.class);

            // Server的内部有个保存 '协议 -> 具体实现' 的HashMap
            // 可以使用反射获取Protocol、Instance，更加灵活，参照RpcServerFactoryPBImpl#getServer
            RPC.Server server = new RPC.Builder(conf)
                    .setProtocol(MyResourceTrackerPB.class)
                    .setInstance(
                            MyResourceTrackerProtocol
                                    .MyResourceTrackerService
                                    .newReflectiveBlockingService(
                                            new MyResourceTrackerPBServiceImpl(this)))
                    .setBindAddress("localhost")
                    .setPort(9999)
                    .setNumHandlers(1)
                    .setVerbose(true)
                    .build();

            // 添加别的服务到Server中，为了举例，我们这里进行覆盖
            addProtocol(
                    conf,
                    MyResourceTrackerPB.class,
                    MyResourceTrackerProtocol
                            .MyResourceTrackerService
                            .newReflectiveBlockingService(
                                    new MyResourceTrackerPBServiceImpl(this)),
                    server);*/

            // RPC.Server采用NIO模型
            server.start();
        }

        /**
         * 添加额外的协议到server中
         */
        void addProtocol(Configuration conf, Class<?> protocol,
                         BlockingService service, RPC.Server server) {
            RPC.setProtocolEngine(conf, protocol, ProtobufRpcEngine.class);
            server.addProtocol(
                    RPC.RpcKind.RPC_PROTOCOL_BUFFER,
                    protocol,
                    service);
        }

        Class<?> getClassByName(Configuration conf, String name) {
            try {
                return conf.getClassByName(name);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        String getPBServiceImplClassName(Class<?> protocol) {
            return "com.ava.bigdata.common.rpc.hadoop.protobuf.server" + "." + protocol.getSimpleName() + "PBServiceImpl";
        }

        String getProtocolClassName(Class<?> protocol) {
            String srcClassName = protocol.getSimpleName();
            return "com.ava.bigdata.common.rpc.hadoop.protobuf.proto" + "." + srcClassName + "Protocol" + "$" +
                    srcClassName + "Service";
        }

        /**
         * 使用反射获取Protocol、Instance，更加灵活，参照RpcServerFactoryPBImpl#getServer
         */
        RPC.Server getServer(final Configuration conf, final Class<?> protocol) throws Exception {
            RPC.Server server;

            // 获取MyResourceTrackerPBServiceImpl对象
            Class<?> serviceImplClazz = getClassByName(conf, getPBServiceImplClassName(protocol));
            Constructor<?> constructor = serviceImplClazz.getConstructor(protocol);
            constructor.setAccessible(true);
            Object service = constructor.newInstance(this);

            // MyResourceTrackerPB
            Class<?> pbProtocol = service.getClass().getInterfaces()[0];

            // MyResourceTrackerProtocol$MyResourceTrackerService
            Class<?> protoClazz = getClassByName(conf, getProtocolClassName(protocol));
            Method method = protoClazz.getMethod("newReflectiveBlockingService",
                    pbProtocol.getInterfaces()[0]);
            method.setAccessible(true);
            BlockingService blockingService = (BlockingService) method.invoke(null, service);

            server = createServer(
                    pbProtocol,
                    blockingService,
                    new InetSocketAddress("localhost", 9999),
                    1,
                    conf);

            // addProtocol(conf, pbProtocol, blockingService, server);
            return server;
        }

        RPC.Server createServer(Class<?> pbProtocol,
                                BlockingService blockingService,
                                InetSocketAddress addr,
                                int numHandlers,
                                Configuration conf) throws IOException {
            RPC.setProtocolEngine(conf, MyResourceTrackerPB.class, ProtobufRpcEngine.class);

            // Server的内部有个保存 '协议 -> 具体实现' 的HashMap
            return new RPC.Builder(conf)
                    .setProtocol(pbProtocol)
                    .setInstance(blockingService)
                    .setBindAddress(addr.getHostName())
                    .setPort(addr.getPort())
                    .setNumHandlers(numHandlers)
                    .setVerbose(true)
                    .build();
        }

        /**
         * MyResourceTracker的具体实现
         */
        @Override
        public MyResourceTrackerMessage.MyRegisterNodeManagerResponseProto registerNodeManager(
                MyResourceTrackerMessage.MyRegisterNodeManagerRequestProto request) throws ServiceException {
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

    public static void main(String[] args) throws Exception {
        // 初始化rpc服务
        new RpcServer(new Configuration());
    }
}
