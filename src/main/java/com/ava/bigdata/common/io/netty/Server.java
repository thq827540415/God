package com.ava.bigdata.common.io.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class Server {

    private static class NettyServerHandler implements ChannelInboundHandler {
        @Override
        public void channelRegistered(ChannelHandlerContext channelHandlerContext) throws Exception {
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext channelHandlerContext) throws Exception {
        }

        @Override
        public void channelActive(ChannelHandlerContext channelHandlerContext) throws Exception {
        }

        @Override
        public void channelInactive(ChannelHandlerContext channelHandlerContext) throws Exception {
        }

        /**
         * Channel读取事件
         * @param o Server接收的到的消息
         */
        @Override
        public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
            ByteBuf byteBuf = (ByteBuf) o;
            System.out.println("Server read msg: " + byteBuf.toString(CharsetUtil.UTF_8));
        }

        /**
         * 读取完毕事件
         */
        @Override
        public void channelReadComplete(ChannelHandlerContext channelHandlerContext) throws Exception {
            // 消息出栈
            channelHandlerContext.writeAndFlush(
                    Unpooled.copiedBuffer("server read complete".getBytes(StandardCharsets.UTF_8))
            );
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext channelHandlerContext) throws Exception {
        }

        /**
         * 异常事件
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext channelHandlerContext, Throwable throwable) throws Exception {
            throwable.printStackTrace();
            channelHandlerContext.close();
        }

        @Override
        public void handlerAdded(ChannelHandlerContext channelHandlerContext) throws Exception {
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext channelHandlerContext) throws Exception {
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // todo 1. 创建BossGroup线程组，用于处理连接事件
        // 默认线程个数为cores * 2，其中1个就够了
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);

        // todo 2. 创建WorkerGroup线程组，用于处理读写事件
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        // todo 3. 创建Server启动助手
        ServerBootstrap serverBootstrap = new ServerBootstrap();

        // todo 4. 设置BossGroup线程组和WorkerGroup线程组
        serverBootstrap.group(bossGroup, workerGroup)
                // todo 5. 设置ServerChannel实现为NIO
                .channel(NioServerSocketChannel.class)
                // todo 6. 参数设置
                // 设置等待连接的队列
                .option(ChannelOption.SO_BACKLOG, 128)
                // 设置channel活跃状态
                .childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE)
                // todo 7. 创建一个Channel初始化对象
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        // todo 8. 向pipeline中添加自定义业务处理handler
                        socketChannel.pipeline().addLast(new NettyServerHandler());
                    }
                });

        // todo 9. 启动服务端并绑定端口，同时将异步改成同步
        ChannelFuture future = serverBootstrap.bind(
                new InetSocketAddress("localhost", 9999)).sync();

        // todo 10. 关闭Channel和连接池
        future.channel().closeFuture().sync();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
