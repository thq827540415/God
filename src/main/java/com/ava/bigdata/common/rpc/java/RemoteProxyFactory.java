package com.ava.bigdata.common.rpc.java;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;

public class RemoteProxyFactory {
    @SuppressWarnings("unchecked")
    public static <T> T createBean(Class<T> service) throws IOException {
        Socket socket = new Socket("localhost", 9999);
        return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[]{service}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeUTF(service.getName());
                oos.writeUTF(method.getName());
                oos.writeObject(method.getParameterTypes());
                oos.writeObject(args);
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                return ois.readObject();
            }
        });
    }
}
