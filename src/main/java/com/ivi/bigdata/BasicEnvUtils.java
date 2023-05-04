package com.ivi.bigdata;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import redis.clients.jedis.JedisPool;

import java.io.IOException;

public class BasicEnvUtils {

    private BasicEnvUtils() {
    }

    /**
     * @return 获取curator客户端
     */
    public static CuratorFramework getCuratorInstance() {
        return CuratorBuilder.CLIENT;
    }

    /**
     * @return 获取HBase客户端
     */
    public static Connection getHBaseInstance() {
        return HBaseBuilder.CLIENT;
    }

    /**
     * @return 获取jedis客户端
     */
    public static JedisPool getJedisPoolInstance() {
        return JedisPoolBuilder.CLIENT;
    }


    private static class CuratorBuilder {
        private static final CuratorFramework CLIENT = CuratorFrameworkFactory.builder()
                .connectString("bigdata01:2181,bigdata02:2181,bigdata03:2181")
                // ExponentialBackoffRetry、RetryNTimes、RetryOneTime、RetryUntilElapsed
                // 第i次重试等待 = baseSleepTimeMs * Math.max(1, random.nextInt(1 << retryCount + 1))
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                // 会话超时时间，默认60000ms
                .sessionTimeoutMs(5000)
                // 连接创建超时时间，默认15000ms
                .connectionTimeoutMs(3000)
                // 所有操作都在/base下执行
                .namespace("base")
                .build();

        static {
            CLIENT.start();
        }
    }

    private static class HBaseBuilder {
        private static final Connection CLIENT;

        static {
            try {
                Configuration conf = HBaseConfiguration.create();
                // read hbase-site.xml
                conf.set("hbase.zookeeper.quorum", "bigdata01,bigdata02,bigdata03");
                conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
                CLIENT = ConnectionFactory.createConnection(conf);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class JedisPoolBuilder {
        private static final JedisPool CLIENT;

        static {
            CLIENT = new JedisPool("localhost", 6379);
        }
    }
}
