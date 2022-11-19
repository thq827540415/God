package com.shadow.garden.bigdata.flink.code.stream.transformation;

import com.shadow.garden.bigdata.consts.UsualConsts;
import com.shadow.garden.bigdata.util.FlinkEnvUtils;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
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
 * 异步获取http数据
 */
public class E12_AsyncIO {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        DataStreamSource<String> source = env.socketTextStream(UsualConsts.NC_HOST, 9999);

        String uri = "";

        AsyncDataStream
                // orderedWait中遵循FIFO
                .unorderedWait(
                        source,
                        new HttpAsyncFunction(uri),
                        3000,
                        TimeUnit.MILLISECONDS,
                        10)
                .print();

        env.execute(E12_AsyncIO.class.getSimpleName());
    }

    private static class HttpAsyncFunction extends RichAsyncFunction<String, Tuple2<String, String>> {

        private transient CloseableHttpAsyncClient httpAsyncClient;

        private final String uri;

        public HttpAsyncFunction(String uri) {
            this.uri = uri;
        }

        @Override
        public void open(Configuration parameters) throws Exception {
            RequestConfig requestConfig = RequestConfig.custom().build();

            httpAsyncClient = HttpAsyncClients.custom()
                    // 异步客户端支持的最大连接数
                    .setMaxConnTotal(10)
                    .setDefaultRequestConfig(requestConfig)
                    .build();
            httpAsyncClient.start();
        }

        @Override
        public void close() throws Exception {
            httpAsyncClient.close();
        }

        @Override
        public void asyncInvoke(String uid, ResultFuture<Tuple2<String, String>> resultFuture) throws Exception {
            HttpGet get = new HttpGet(uri + "/?uid=" + uid);
            Future<HttpResponse> future = httpAsyncClient.execute(get, null);

            CompletableFuture
                    .supplyAsync(
                            new Supplier<String>() {
                                @Override
                                public String get() {
                                    try {
                                        HttpResponse response = future.get();
                                        String res = "";
                                        if (response.getStatusLine().getStatusCode() == 200) {
                                            res = EntityUtils.toString(response.getEntity());
                                        }
                                        return res;
                                    } catch (InterruptedException | ExecutionException | IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            })
                    .thenAccept(
                            new Consumer<String>() {
                                @Override
                                public void accept(String s) {
                                    resultFuture.complete(Collections.singleton(Tuple2.of(uid, s)));
                                }
                            });
        }
    }
}
