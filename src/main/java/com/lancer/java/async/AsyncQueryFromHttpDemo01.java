package com.lancer.java.async;

import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 异步IO是用来查询外部的数据库、或API(REST即HTTP的api) 关联维度数据
 * 1.数据太多，无法缓存到内存或者使用BroadcastState
 * 2.必须查别人的API，获取到需要关联的数据
 *
 * 同步查询：来一条查一次，太慢了，会出现性能瓶颈，在一个subtask中请求是串行的
 *
 * 异步查询：开多线程查，查询快，但是消耗资源多
 */
public class AsyncQueryFromHttpDemo01 {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 设置重启策略
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, 5000));

        DataStreamSource<String> source = env.socketTextStream("bigdata01", 9999);
        String url = "http://localhost:8080/api"; // 异步IO发生REST请求的地址
        int capacity = 20; // 最大异步并发请求数量

        // 使用AsyncDataStream调用unorderedWait方法，并传入异步请求的Function
        // unorderedWait发送的请求和响应的结果是没有顺序的
        // orderedWait发送的请求和响应的结果是有顺序的，先请求的先返回
        SingleOutputStreamOperator<Tuple2<String, String>> result = AsyncDataStream.unorderedWait(
                source, // 输入的数据流
                new HttpAsyncFunction(url, capacity), // 异步查询的Function实例
                5000, // 超时时间
                TimeUnit.MILLISECONDS,
                capacity); // 异步请求队列最大的数量，不传入该参数，默认值为100

        result.print();

        env.execute();
    }

    private static class HttpAsyncFunction extends RichAsyncFunction<String, Tuple2<String, String>> {

        private transient CloseableHttpAsyncClient httpClient; // 异步请求的HttpClient
        private final String url; // 请求的URL地址
        private final int maxConnTotal; // 异步HTTPClient支持的最大连接

        public HttpAsyncFunction(String url, int maxConnTotal) {
            this.url = url;
            this.maxConnTotal = maxConnTotal;
        }

        @Override
        public void open(Configuration parameters) throws Exception {
            RequestConfig requestConfig = RequestConfig.custom().build();
            httpClient = HttpAsyncClients.custom() // 创建HttpAsyncClients请求连接池
                    .setMaxConnTotal(maxConnTotal) // 设置最大连接数
                    .setDefaultRequestConfig(requestConfig).build();
            httpClient.start(); // 启动异步请求httpClient
        }

        @Override
        public void asyncInvoke(String uid, ResultFuture<Tuple2<String, String>> resultFuture) throws Exception {
            HttpGet httpGet = new HttpGet(url + "/?uid=" + uid); // 请求的地址和参数
            Future<HttpResponse> future = httpClient.execute(httpGet, null); // 执行请求，返回future
            CompletableFuture.supplyAsync(new Supplier<String>() {
                @Override
                public String get() {
                    try {
                        HttpResponse response = future.get(); // 调用Future的get方法获取请求的结果
                        String res = null;
                        if (response.getStatusLine().getStatusCode() == 200) {
                            res = EntityUtils.toString(response.getEntity());
                        }
                        return res;
                    } catch (InterruptedException | ExecutionException | IOException e) {
                        return null;
                    }
                }
            }).thenAccept(new Consumer<String>() {
                @Override
                public void accept(String result) {
                    // 将结果添加到resultFuture中输出（complete方法的参数只能为集合，如果只有一个元素，就返回单例集合）
                    resultFuture.complete(Collections.singleton(Tuple2.of(uid, result)));
                }
            });
        }

        @Override
        public void close() throws Exception {
            httpClient.close();
        }
    }
}
