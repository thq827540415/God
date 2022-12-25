package com.ava.bigdata.common.io;

import lombok.Cleanup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

/**
 * 主要有3大组件：ByteBuffer、Channel、Selector
 */
public class MyNIO {
    private static class MyByteBuffer {
        private static void createByteBuffer() {
            // 创建指定长度的Buffer
            ByteBuffer byteBuffer = ByteBuffer.allocate(10);
            for (int i = 0; i < byteBuffer.capacity(); i++) {
                System.out.print(byteBuffer.get(i) + " ");
            }

            System.out.println("\n======================");

            ByteBuffer wrap = ByteBuffer.wrap("bytebuffer".getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < wrap.capacity(); i++) {
                System.out.print(wrap.get(i) + " ");
            }
        }

        public static void main(String[] args) {
            createByteBuffer();
        }
    }

    private static class MyBIO {
        public static void main(String[] args) {
            try (ServerSocket serverSocket = new ServerSocket(9999)) {
                Socket clientSocket = serverSocket.accept();
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                String request, response;
                while ((request = br.readLine()) != null) {
                    if ("Done".equals(request)) {
                        break;
                    }
                    response = "response";
                    System.out.println(response);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class MySocketChannel {
        private static class MyServerSocketChannel {
            private static volatile boolean flag = true;
            public static void main(String[] args) {
                try (ServerSocketChannel channel = ServerSocketChannel.open()) {
                    // 绑定对应的端口
                    channel.socket().bind(new InetSocketAddress("localhost", 9999));

                    // 默认channel是阻塞的
                    channel.configureBlocking(false);

                    // 创建channel selector
                    @Cleanup Selector selector = Selector.open();

                    // 将服务端channel注册到selector，并指定注册监听的事件为OP_ACCEPT
                    channel.register(selector, SelectionKey.OP_ACCEPT);
                    System.out.println("server start successful");

                    while (flag) {
                        // 检查selector是否有事件，阻塞等待2s，返回值是事件个数
                        int eventNums = selector.select(1000);
                        // System.out.println("server hasn't client");
                        // CommonUtils.sleep(3, TimeUnit.SECONDS);

                        if (eventNums == 0) {
                            System.out.println("no events");
                        } else {
                            // 监听到事件后，这里将监听的服务端通道选中
                            Iterator<SelectionKey> events = selector.selectedKeys().iterator();
                            while (events.hasNext()) {
                                SelectionKey key = events.next();
                                // 判断事件是否是客户端的连接事件
                                if (key.isAcceptable()) {
                                    SocketChannel acceptChannel = channel.accept();
                                    System.out.println("There is a client connecting...");

                                    // 得到客户端通道，并将其注册到选择器上，并指定监听事件为OP_READ
                                    acceptChannel.configureBlocking(false);
                                    acceptChannel.register(selector, SelectionKey.OP_READ);
                                }

                                if (key.isReadable()) {
                                    // 获取该key对应的channel，也就是上面注册为OP_READ的channel
                                    @Cleanup SocketChannel acceptChannel = (SocketChannel) key.channel();
                                    // 得到客户端通道，读取数据到缓冲区
                                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                                    int read = acceptChannel.read(buffer);
                                    if (read > 0) {
                                        // 服务端接收到客户端的数据
                                        System.out.println("server accept client's message: " +
                                                new String(buffer.array(), 0, read, StandardCharsets.UTF_8));

                                        // 向客户端返回消息
                                        acceptChannel.write(
                                                ByteBuffer.wrap(
                                                        "server already accept message".getBytes(StandardCharsets.UTF_8)));
                                    }
                                }

                                // 第一次将ServerSocketChannel的OP_ACCEPECT事件清除
                                // 第二次将SocketChannel的OP_READ事件清除
                                events.remove();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    flag = false;
                }
            }
        }

        private static class MyClientSocketChannel {
            public static void main(String[] args) {
                try (SocketChannel channel =
                             SocketChannel.open(new InetSocketAddress("localhost", 9999))) {

                    // channel.configureBlocking(false);

                    // 写出数据
                    channel.write(
                            ByteBuffer.wrap(
                                    "this is client and i send a message".getBytes(StandardCharsets.UTF_8)));

                    // 用于接收服务端发送的消息
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    int read = channel.read(buffer);
                    if (read > 0) {
                        System.out.println("client receive server's message: " +
                                new String(buffer.array(), 0, read, StandardCharsets.UTF_8));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
