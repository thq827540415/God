package com.ava.basic.nio;

import com.ava.util.CommonUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

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
                    channel.bind(new InetSocketAddress("localhost", 9999));

                    // 默认channel是阻塞的
                    channel.configureBlocking(false);
                    System.out.println("server start successful");

                    while (flag) {
                        SocketChannel acceptChannel = channel.accept();
                        if (acceptChannel == null) {
                            System.out.println("server hasn't client");
                            CommonUtils.sleep(3, TimeUnit.SECONDS);
                        } else {
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            int read = acceptChannel.read(buffer);
                            System.out.println("server accept client's message: " +
                                    new String(buffer.array(), 0, read, StandardCharsets.UTF_8));

                            acceptChannel.write(
                                    ByteBuffer.wrap(
                                            "server already accept message".getBytes(StandardCharsets.UTF_8)));
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
                    // 写出数据
                    channel.write(
                            ByteBuffer.wrap(
                                    "this is client and i send a message".getBytes(StandardCharsets.UTF_8)));

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
