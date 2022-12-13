package com.ava.bigdata.common.rpc;

public interface BusinessProtocol {
    long versionID = 345043000L;
    void mkdir(String path);
    String getName(String name);
}
