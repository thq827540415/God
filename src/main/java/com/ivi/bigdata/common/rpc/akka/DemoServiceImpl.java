package com.ivi.bigdata.common.rpc.akka;

public class DemoServiceImpl implements DemoService{
    @Override
    public String sayHello(String name) {
        return "This is akka RPC service. Hello " + name;
    }

    @Override
    public String sayGoodbye(String name) {
        return "This is akka RPC service. Goodbye " + name;
    }
}
