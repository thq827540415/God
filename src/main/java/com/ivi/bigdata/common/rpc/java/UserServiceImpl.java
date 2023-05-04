package com.ivi.bigdata.common.rpc.java;

public class UserServiceImpl implements UserService {
    @Override
    public String saveUser(User user) {
        System.out.println("新增用户: " + user);
        return "success";
    }
}
