package com.ivi.bigdata.common.rpc.java;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ServerSide {
    public static void main(String[] args) throws Exception {
        try (ServerSocket server = new ServerSocket(9999)) {
            HashMap<String, Object> beanPool = new HashMap<>();
            beanPool.put("com.ava.bigdata.common.rpc.java.UserService", new UserServiceImpl());
            while (true) {
                Socket client = server.accept();
                ObjectInputStream ois = new ObjectInputStream(client.getInputStream());

                String serviceName = ois.readUTF();
                String methodName = ois.readUTF();
                Class<?>[] paramType = (Class<?>[]) ois.readObject();
                Object[] params = (Object[]) ois.readObject();

                Object bean = beanPool.get(serviceName);
                Method method = bean.getClass().getMethod(methodName, paramType);
                Object result = method.invoke(bean, params);
                ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
                oos.writeObject(result);
            }
        }
    }
}
