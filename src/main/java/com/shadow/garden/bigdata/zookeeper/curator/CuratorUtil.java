package com.shadow.garden.bigdata.zookeeper.curator;

import com.shadow.garden.bigdata.consts.Consts;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

public class CuratorUtil {
    private CuratorUtil() {
    }

    public static CuratorFramework getInstance() {
        return Builder.CLIENT;
    }

    private static class Builder {
        private static final CuratorFramework CLIENT = CuratorFrameworkFactory.builder()
                .connectString(Consts.ZK_CONN_STR)
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
    }
}
