package com.ava.bigdata.common.io.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Client {
    private static class NettyClientHandler implements ChannelInboundHandler {
        @Override
        public void channelRegistered(ChannelHandlerContext channelHandlerContext) throws Exception {
        }

        @Override
        public void channelUnregistered(ChannelHandlerContext channelHandlerContext) throws Exception {
        }

        /**
         * channel就绪事件
         */
        @Override
        public void channelActive(ChannelHandlerContext channelHandlerContext) throws Exception {
            channelHandlerContext.writeAndFlush(
                    Unpooled.copiedBuffer("client is already", Charset.defaultCharset())
            );
        }

        @Override
        public void channelInactive(ChannelHandlerContext channelHandlerContext) throws Exception {
        }

        /**
         * Channel读取事件
         * @param o Client接收的到的消息
         */
        @Override
        public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
            ByteBuf byteBuf = (ByteBuf) o;
            System.out.println("Client read msg: " + byteBuf.toString(CharsetUtil.UTF_8));
        }

        /**
         * 读取完毕事件
         */
        @Override
        public void channelReadComplete(ChannelHandlerContext channelHandlerContext) throws Exception {
            // 消息出栈
            channelHandlerContext.writeAndFlush(
                    Unpooled.copiedBuffer("client read complete".getBytes(StandardCharsets.UTF_8))
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
        NioEventLoopGroup loopGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();

        bootstrap.group(loopGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(new NettyClientHandler());
                    }
                });

        ChannelFuture future = bootstrap.connect("localhost", 9999).sync();

        future.channel().closeFuture().sync();
        loopGroup.shutdownGracefully();
    }
}
