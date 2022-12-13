package com.ava.bigdata.common.rpc;

/**
 * client请求调用的服务组件
 */
public class BusinessImpl implements BusinessProtocol {
    @Override
    public void mkdir(String path) {
        System.out.println("成功创建了文件夹：" + path);
    }

    @Override
    public String getName(String name) {
        System.out.println("成功打了招呼：hello：" + name);
        return "getName";
    }
}
