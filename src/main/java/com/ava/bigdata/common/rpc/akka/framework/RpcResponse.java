package com.ava.bigdata.common.rpc.akka.framework;

import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@ToString
public class RpcResponse implements Serializable {
    private static final long serialVersionUID = 8507384712953257082L;

    public static final String SUCCESS = "succeed";

    public static final String FAILED = "failed";

    private String status = RpcResponse.SUCCESS;

    private String message;

    private Object data;

    public RpcResponse setStatus(String status) {
        this.status = status;
        return this;
    }

    public RpcResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    public RpcResponse setData(Object data) {
        this.data = data;
        return this;
    }
}
