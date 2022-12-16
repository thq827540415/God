package com.ava.bigdata.common.rpc.java;

import java.io.IOException;

public class ClientSide {
    public static void main(String[] args) throws IOException {
        UserService userService = RemoteProxyFactory.createBean(UserService.class);
        String result = userService.saveUser(new User("zs", 19));
        System.out.println("result: " + result);
    }
}
