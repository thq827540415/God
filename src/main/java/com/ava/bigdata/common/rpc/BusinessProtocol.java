package com.ava.bigdata.common.rpc;

/**
 * 服务端的定义的协议，协议就是一个interface
 */
public interface BusinessProtocol {
    long versionID = 345043000L;
    void mkdir(String path);
    String getName(String name);
}
