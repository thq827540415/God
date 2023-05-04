package com.ivi.grammar;

import java.sql.DriverManager;
import java.util.ServiceLoader;

/**
 * @Author lancer
 * @Date 2023/3/9 12:35
 * @Description JDK提供的一个加载服务的简单机制ServiceLoader。
 * 使用ServiceLoader从/resources/META-INF/services目录下加载符合一个公共接口的服务。
 * 目录下格式为  名字为'服务的权限定类名'的文本文件 + 文件内容为实现该服务的实现类的权限定类名
 * JDK9使用模块来简化这种服务发现方式
 */
public class ServiceLoaderLearn {

    /**
     * 公共接口可以是interface，也可以是abstract class，甚至是class，只要是抽象出来的父类就行
     */
    interface Cipher {
        byte[] encrypt(byte[] source, byte[] key);

        byte[] decrypt(byte[] source, byte[] key);

        int strength();
    }

    /**
     * 实现类可以放在任意的包中，每个实现类必须有一个无参构造器
     */
    public static class CaesarCipher implements Cipher {
        @Override
        public byte[] encrypt(byte[] source, byte[] key) {
            byte[] result = new byte[source.length];
            for (int i = 0; i < source.length; i++) {
                result[i] = (byte) (source[i] + key[0]);
            }
            return result;
        }

        @Override
        public byte[] decrypt(byte[] source, byte[] key) {
            return encrypt(source, new byte[]{(byte) -key[0]});
        }

        @Override
        public int strength() {
            return 1;
        }
    }

    public static void main(String[] args) {

        System.out.println(DriverManager.getDrivers());

        // 初始化服务加载器
        ServiceLoader<Cipher> loader = ServiceLoader.load(Cipher.class);
        // JDK9提供了一个Stream<ServiceLoader.Provider<S>> stream()方法，
        // Provider提供了Class<? extends S> type()和S get两个方法,也是JDK9才有。
        // JDK9还提供了一个Optional<S> findFirst()
        loader.forEach(cipher -> {
            if (cipher.strength() >= 1) {
                System.out.println(cipher);
            }
        });

    }
}