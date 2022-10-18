package com.lancer.flink.java.window.timewindow.eventtime;

import org.apache.flink.api.common.eventtime.*;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.datastream.WindowedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class TumblingEventTimeWindowKeyedDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(new Configuration());

        test01(env);

        env.execute();
    }

    // 老版API
    private static void test01(StreamExecutionEnvironment env) {

        env.setParallelism(2);

        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        // 1000,spark
        // 2000,hadoop
        // 5000,spark
        // 3000,hadoop --> 迟到数据默认被抛弃了
        DataStreamSource<String> source = env.socketTextStream("bigdata01", 9999);

        SingleOutputStreamOperator<String> linesWithWaterMark = source.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<String>(Time.seconds(0)) { // 达到WaterMark时，延迟Time.seconds(s)后触发
            @Override
            public long extractTimestamp(String element) {
                return Long.parseLong(element.split(",")[0].trim());
            }
        }).setParallelism(2);


        WindowedStream<Tuple2<String, Integer>, String, TimeWindow> tumblingEventTimeWindow = linesWithWaterMark
                .map(value -> Tuple2.of(value.split(",")[1].trim(), 1), TypeInformation.of(new TypeHint<Tuple2<String, Integer>>() {
                }))
                .keyBy(value -> value.f0)
                .window(TumblingEventTimeWindows.of(Time.of(5, TimeUnit.SECONDS)));

        SingleOutputStreamOperator<Tuple2<String, Integer>> sumDataStream = tumblingEventTimeWindow.sum(1);

        sumDataStream.print();
    }

    // 老版API
    private static void test02(StreamExecutionEnvironment env) {

        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        // 1000,spark
        // 2000,hadoop
        // 5000,spark
        // 3000,hadoop --> 迟到数据默认被抛弃了
        DataStreamSource<String> source = env.socketTextStream("bigdata01", 9999);

        SingleOutputStreamOperator<Tuple3<Long, String, Integer>> mapDataStream = source
                .map(value -> Tuple3.of(Long.parseLong(value.split(",")[0].trim()), value.split(",")[1].trim(), 1), TypeInformation.of(new TypeHint<Tuple3<Long, String, Integer>>() {
                }));

        SingleOutputStreamOperator<Tuple3<Long, String, Integer>> linesWithWaterMark =
                mapDataStream.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<Tuple3<Long, String, Integer>>(Time.seconds(0)) { // 达到WaterMark时，延迟Time.seconds(s)后触发
                    @Override
                    public long extractTimestamp(Tuple3<Long, String, Integer> element) {
                        return element.f0;
                    }
                }).setParallelism(1); // 多个并行的水位线就不一样了，需要触发边界的次数 = WM的并行度(每个subtask都触发一次，或者某个subtask触发两次)。每条记录的水位线都会向后发送

        // 水位线计算 = 进入这个subtask中最大的event time - 延迟时间  比如, 1000,spark --> 这条记录的水位线为 1000ms - Time.seconds(0) = 1000ms
        WindowedStream<Tuple3<Long, String, Integer>, String, TimeWindow> tumblingEventTimeWindow = linesWithWaterMark
                .keyBy(value -> value.f1)
                .window(TumblingEventTimeWindows.of(Time.of(5, TimeUnit.SECONDS)));

        SingleOutputStreamOperator<Tuple3<Long, String, Integer>> sumDataStream = tumblingEventTimeWindow.sum(2);

        sumDataStream.print();
    }

    // 新版KafkaSource
    // 支持topic通配符，消费多个topic
    public static void test03(StreamExecutionEnvironment env) throws IOException {
        env.setParallelism(2);

        // env.getConfig().setAutoWatermarkInterval(2000); // 设置调用getCurrentWatermark()方法的时间，默认200ms，该方法会周期性调用，隔一段时间一直调用
        Properties p = new Properties();
        p.load(TumblingEventTimeWindowKeyedDemo.class.getClassLoader().getResourceAsStream("consumer.properties"));

        /*String serverStr = p.getProperty("bootstrap.servers");
        String group = p.getProperty("group.id");*/

        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
                /*.setBootstrapServers(serverStr)
                .setGroupId(group)*/
                .setTopics("windowTest")
                .setProperties(p)
                .setStartingOffsets(OffsetsInitializer.latest()) // 默认earliest
                /*.setDeserializer(new KafkaRecordDeserializationSchema<String>() {
                    @Override
                    public void deserialize(ConsumerRecord<byte[], byte[]> record, Collector<String> out) throws IOException {
                        out.collect(new String(record.value(), StandardCharsets.UTF_8));
                    }

                    @Override
                    public TypeInformation<String> getProducedType() {
                        return Types.STRING;
                    }
                })*/
                /*.setDeserializer(KafkaRecordDeserializationSchema.of(new KafkaDeserializationSchema<String>() {
                    @Override
                    public boolean isEndOfStream(String nextElement) {
                        return false;
                    }

                    @Override
                    public String deserialize(ConsumerRecord<byte[], byte[]> record) throws Exception {
                        return new String(record.value(), StandardCharsets.UTF_8);
                    }

                    @Override
                    public TypeInformation<String> getProducedType() {
                        return TypeInformation.of(String.class);
                    }
                }))*/
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 1000,spark,2
        // 1000,hadoop,1
        // 4999,spark,1
        // 4999,hadoop,1
        /*DataStreamSource<String> source = env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "kafkaSource");
        SingleOutputStreamOperator<String> lineWithWaterMark = source.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<String>(Time.seconds(0)) {
            @Override
            public long extractTimestamp(String element) {
                return Long.parseLong(element.split(",")[0].trim());
            }
        });

        source.assignTimestampsAndWatermarks(WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofMillis(0)).withTimestampAssigner(((element, recordTimestamp) -> Long.parseLong(element.split(",")[0].trim()))));*/

        // 只有整点才会触发，例如4999不会触发，得要5000才会触发 （watermark = 5000 - 0 - 1 = 4999的窗口边界值）
        DataStreamSource<String> lineWithWaterMark = env.fromSource(kafkaSource,
                WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofMillis(0))
                        .withTimestampAssigner((element, recordTimestamp) -> Long.parseLong(element.split(",")[0].trim())), "kafkaSource");
        WindowedStream<Tuple2<String, Integer>, String, TimeWindow> windowedStream = lineWithWaterMark
                .map(new MapFunction<String, Tuple2<String, Integer>>() {
                    @Override
                    public Tuple2<String, Integer> map(String value) throws Exception {
                        String[] words = value.split(",");
                        return Tuple2.of(words[1], Integer.parseInt(words[2]));
                    }
                })
                .keyBy(new KeySelector<Tuple2<String, Integer>, String>() {
                    @Override
                    public String getKey(Tuple2<String, Integer> value) throws Exception {
                        return value.f0;
                    }
                })
                .window(TumblingEventTimeWindows.of(Time.seconds(5)));

        SingleOutputStreamOperator<Tuple2<String, Integer>> sumStream = windowedStream.sum(1);

        sumStream.print();
    }

    // 泛型方法返回值前面的<T>表示对后面的泛型的约束
    public static <T extends Integer> void add(T a, T b) {
        System.out.println(b);
        System.out.println(b);
    }
}
