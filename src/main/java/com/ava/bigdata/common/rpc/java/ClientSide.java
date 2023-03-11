package com.ava.bigdata.common.rpc.java;

import java.io.IOException;
import java.net.Socket;

public class ClientSide {
    public static void main(String[] args) throws IOException {
        // 还不能关闭，得等待Server返回数据
        try (Socket socket = new Socket("localhost", 9999)) {
            UserService userService = RemoteProxyFactory.createBean(UserService.class, socket);
            String result = userService.saveUser(new User("zs", 19));
            System.out.println("result: " + result);
        }
    }
}
