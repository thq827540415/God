package com.ivi.bigdata.common.rpc.akka.framework;

import lombok.Getter;

import java.io.Serializable;
import java.util.Arrays;

@Getter
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 3124128471294567182L;

    private String methodName;

    private Object[] parameters;

    private Class<?>[] parameterTypes;

    public RpcRequest setMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    public RpcRequest setParameters(Object[] parameters) {
        this.parameters = parameters;
        return this;
    }


    public RpcRequest setParameterTypes(Class<?>[] parameterTypes) {
        this.parameterTypes = parameterTypes;
        return this;
    }

    @Override
    public String toString() {
        return "RpcRequest {" +
                "methodName = '" + methodName + "\'" +
                ", parameters = " + Arrays.toString(parameters) +
                ", parameterTypes = " + Arrays.toString(parameterTypes) +
                "}";
    }
}
