package com.lancer.java.async;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
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

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
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
public class AsyncQueryFromHttpDemo02 {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // 设置重启策略
        env.setRestartStrategy(RestartStrategies.fixedDelayRestart(3, 5000));

        /**
         * {"oid":"o124", "cid": 2, "money": 200.0, "longitude":117.397128,"latitude":38.916527}
         * {"oid":"o125", "cid": 3, "money": 100.0, "longitude":118.397128,"latitude":35.916527}
         * {"oid":"o127", "cid": 1, "money": 100.0, "longitude":116.395128,"latitude":39.916527}
         * {"oid":"o128", "cid": 2, "money": 200.0, "longitude":117.396128,"latitude":38.916527}
         * {"oid":"o129", "cid": 3, "money": 300.0, "longitude":115.398128,"latitude":35.916527}
         * {"oid":"o130", "cid": 2, "money": 100.0, "longitude":116.397128,"latitude":39.916527}
         * {"oid":"o131", "cid": 1, "money": 100.0, "longitude":117.394128,"latitude":38.916527}
         * {"oid":"o132", "cid": 3, "money": 200.0, "longitude":118.396128,"latitude":35.916527}
         */
        DataStreamSource<String> source = env.socketTextStream("bigdata01", 9999);
        String url = "https://restapi.amap.com/v3/geocode/regeo"; // 异步请求高德地图的地址
        String key = "4924f7ef5c86a278f5500851541cdcff"; // 请求高德地图的密钥
        int capacity = 50; // 最大异步并发请求数量
        // 使用AsyncDataStream调用unorderedWait方法，并传入异步请求的Function
        // unorderedWait发送的请求和响应的结果是没有顺序的
        // orderedWait发送的请求和响应的结果是有顺序的，先请求的先返回

        SingleOutputStreamOperator<LogBean> result = AsyncDataStream.unorderedWait(
                source,
                new AsyncHttpGeoQueryFunction(url, key, capacity),
                3000,
                TimeUnit.MILLISECONDS,
                capacity);

        result.print();

        env.execute();
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @ToString
    public static class LogBean {
        private String uid;

        private Double longitude;

        private Double latitude;

        private String province;

        private String city;

        public LogBean(String uid, Double longitude, Double latitude) {
            this.uid = uid;
            this.longitude = longitude;
            this.latitude = latitude;
        }

        public static LogBean of(String uid, Double longitude, Double latitude) {
            return new LogBean(uid, longitude, latitude);
        }

        /*@Override
        public String toString() {
            return "LogBean{" +
                    "uid='" + uid + '\'' +
                    ", longitude=" + longitude +
                    ", latitude=" + latitude +
                    ", province='" + province + '\'' +
                    ", city='" + city + '\'' +
                    '}';
        }*/
    }

    private static class AsyncHttpGeoQueryFunction extends RichAsyncFunction<String, LogBean> {

        private transient CloseableHttpAsyncClient httpclient; //异步请求的HttpClient
        private String url; //请求高德地图URL地址
        private String key; //请求高德地图的秘钥，注册高德地图开发者后获得
        private final int maxConnTotal; //异步HTTPClient支持的最大连接
        public AsyncHttpGeoQueryFunction(String url, String key, int maxConnTotal) {
            this.url = url;
            this.key = key;
            this.maxConnTotal = maxConnTotal;
        }

        /**
         * 初始化异步http请求的连接池
         * @param parameters
         * @throws Exception
         */
        @Override
        public void open(Configuration parameters) throws Exception {
            RequestConfig requestConfig = RequestConfig.custom().build();
            httpclient = HttpAsyncClients.custom()
                    .setMaxConnTotal(maxConnTotal)
                    .setDefaultRequestConfig(requestConfig).build();
            httpclient.start();
        }

        @Override
        public void asyncInvoke(String line, ResultFuture<LogBean> resultFuture) throws Exception {
            // 使用fastjson将字符串解析成json对象
            LogBean bean = JSON.parseObject(line, LogBean.class);
            Double longitude = bean.longitude; // 获取经度
            Double latitude = bean.latitude; // 获取纬度
            // 将经纬度和高德地图的key与请求的url进行拼接
            HttpGet httpGet = new HttpGet(url + "?location=" + longitude + "," + latitude + "&key=" + key);
            // 发送异步请求，返回Future
            Future<HttpResponse> future = httpclient.execute(httpGet, null);
            CompletableFuture.supplyAsync(new Supplier<LogBean>() {
                @Override
                public LogBean get() {
                    try {
                        HttpResponse response = future.get();
                        String province = null;
                        String city = null;
                        if (response.getStatusLine().getStatusCode() == 200) {
                            // 解析返回的结果，获取省份、城市等信息
                            String result = EntityUtils.toString(response.getEntity());
                            JSONObject jsonObject = JSON.parseObject(result);
                            JSONObject regeocode = jsonObject.getJSONObject("regeocode");
                            if (regeocode != null && !regeocode.isEmpty()) {
                                JSONObject address = regeocode.getJSONObject("addressComponent");
                                province = address.getString("province");
                                city = address.getString("city");
                            }
                        }
                        bean.setProvince(province);
                        bean.setCity(city);
                        return bean;
                    } catch (Exception e) {
                        return null;
                    }
                }
            }).thenAccept(new Consumer<LogBean>() {
                @Override
                public void accept(LogBean result) {
                    resultFuture.complete(Collections.singleton(result));
                }
            });
        }

        @Override
        public void close() throws Exception {
            httpclient.close();
        }
    }
}
