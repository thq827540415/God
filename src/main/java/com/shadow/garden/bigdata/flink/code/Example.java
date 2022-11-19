package com.shadow.garden.bigdata.flink.code;

import com.shadow.garden.bigdata.util.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.jdbc.JdbcStatementBuilder;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * 需要额外导入的依赖
 * 自动生成构造，toString等
 * <dependency>
 * <groupId>org.projectlombok</groupId>
 * <artifactId>lombok</artifactId>
 * <version>1.18.22</version>
 * <optional>true</optional>
 * </dependency>
 * 解析JSON
 * <dependency>
 * <groupId>com.alibaba</groupId>
 * <artifactId>fastjson</artifactId>
 * <version>1.2.75</version>
 * </dependency>
 * 使用java
 * <dependency>
 * <groupId>org.apache.flink</groupId>
 * <artifactId>flink-streaming-java_${scala.binary.version}</artifactId>
 * <version>${flink.version}</version>
 * <scope>${flink.scope}</scope>
 * </dependency>
 */
public class Example {

    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderMain {
        public int orderId;
        public String orderSn;
        public int customerId;
        public String shippingUser;
        public String province;
        public String city;
        public String address;
        public int orderSource;
        public int paymentMethod;
        public double orderMoney;
        public double districtMoney;
        public double shippingMoney;
        public double paymentMoney;
        public String shippingComName;
        public String shippingSn;
        public String createTime;
        public String shippingTime;
        public String payTime;
        public String receiveTime;
        public String orderStatus;
        public int orderPoint;
        public String invoiceTime;
        public Timestamp modifiedTime;
    }

    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDetail {
        public int orderDetailId;
        public String orderSn;
        public int productId;
        public String productName;
        public int productCnt;
        public double productPrice;
        public float averageCost;
        public float weight;
        public double feeMoney;
        public int wId;
        public String createTime;
        public Timestamp modifiedTime;
    }

    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Result {
        public long sn;
        public double orderPrice;
        public int orderDetailCount;
    }

    private static class Consts {
        public static final String KAFKA_BOOTSTRAP_SERVER = "bigdata01:9092,bigdata02:9092,bigdata03:9092";

        public static final String CLICKHOUSE_DRIVER = "ru.yandex.clickhouse.ClickHouseDriver";

        public static final String CLICKHOUSE_URL = "jdbc:clickhouse://clickhouse_hostname:8123/shtd_result";

        public static final String CLICKHOUSE_USERNAME = "default";

        public static final String CLICKHOUSE_PASSWORD = "123456";
    }

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // following tow-lines are used to test
        env.setParallelism(1);
        env.setMaxParallelism(4);

        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

        tableEnv.executeSql(
                "create temporary table order_main (" +
                        "       order_id int," +
                        "       order_sn string," +
                        "       customer_id int," +
                        "       shipping_user string," +
                        "       province string," +
                        "       city string," +
                        "       address string," +
                        "       order_source int," +
                        "       payment_method int," +
                        "       order_money double," +
                        "       district_money double," +
                        "       shipping_money double," +
                        "       payment_money double," +
                        "       shipping_comp_name string," +
                        "       shipping_sn string," +
                        "       create_time string," +
                        "       shipping_time string," +
                        "       pay_time string," +
                        "       receive_time string," +
                        "       order_status string," +
                        "       order_point int," +
                        "       invoice_time string," +
                        "       modified_time timestamp(3)," +
                        "       watermark for modified_time as modified_time - interval '0' second" +
                        ") with (" +
                        "       'connector' = 'kafka'," +
                        "       'topic' = 'order-main'," +
                        "       'properties.bootstrap.servers' = 'bigdata01:9092,bigdata02:9092,bigdata03:9092'," +
                        "       'properties.group.id' = 'test-group1'," +
                        "       'scan.startup.mode' = 'latest-offset'," +
                        "       'value.format' = 'json'" +
                        ")");

        tableEnv.executeSql(
                "create temporary table order_detail (" +
                        "       order_detail_id int," +
                        "       order_sn string," +
                        "       product_id int," +
                        "       product_name string," +
                        "       product_cnt int," +
                        "       product_price double," +
                        "       average_cost float," +
                        "       weight float," +
                        "       fee_money float," +
                        "       w_id int," +
                        "       create_time string," +
                        "       modified_time timestamp(3)," +
                        "       watermark for modified_time as modified_time - interval '0' second" +
                        ") with (" +
                        "       'connector' = 'kafka'," +
                        "       'topic' = 'order-detail'," +
                        "       'properties.bootstrap.servers' = 'bigdata01:9092,bigdata02:9092,bigdata03:9092'," +
                        "       'properties.group.id' = 'test-group2'," +
                        "       'scan.startup.mode' = 'latest-offset'," +
                        "       'value.format' = 'json'" +
                        ")");

        // 方式一
        withDS(env);
        // 方式二
        // withTableAndDS(tableEnv);
        // 方式三
        // withTableApi(tableEnv);

        env.execute(Example.class.getSimpleName());
    }

    /**
     * 完全使用ds，需要手动解析json串
     */
    private static void withDS(StreamExecutionEnvironment env) {
        SingleOutputStreamOperator<OrderMain> orderMainDataStream = env
                .fromSource(
                        getKafkaSource("order-main", "test-group1"),
                        WatermarkStrategy
                                .<String>forMonotonousTimestamps()
                                .withTimestampAssigner(new SerializableTimestampAssigner<String>() {
                                    @Override
                                    public long extractTimestamp(String element, long recordTimestamp) {
                                        OrderMain orderMain = JsonUtils.parseObject(element, OrderMain.class);
                                        return orderMain.modifiedTime.getTime();
                                    }
                                }),
                        "kafka1")
                .map(line -> JsonUtils.parseObject(line, OrderMain.class))
                .returns(Types.POJO(OrderMain.class));

        SingleOutputStreamOperator<OrderDetail> orderDetailDataStream = env
                .fromSource(
                        getKafkaSource("order-detail", "test-group2"),
                        WatermarkStrategy
                                .<String>forMonotonousTimestamps()
                                .withTimestampAssigner(new SerializableTimestampAssigner<String>() {
                                    @Override
                                    public long extractTimestamp(String element, long recordTimestamp) {
                                        OrderDetail orderDetail1 = JsonUtils.parseObject(element, OrderDetail.class);
                                        return orderDetail1.modifiedTime.getTime();
                                    }
                                }),
                        "kafka2")
                .map(line -> JsonUtils.parseObject(line, OrderDetail.class))
                .returns(Types.POJO(OrderDetail.class));

        doTask(orderMainDataStream, orderDetailDataStream);
    }

    /**
     * 如果数据源使用table，逻辑使用ds，表中生成的Watermark没用，需要手动注册Watermark
     */
    private static void withTableAndDS(StreamTableEnvironment tableEnv) {
        SingleOutputStreamOperator<OrderMain> orderMainDataStream = tableEnv
                .toDataStream(
                        tableEnv.sqlQuery("select * from order_main"),
                        OrderMain.class)
                .assignTimestampsAndWatermarks(WatermarkStrategy.
                        <OrderMain>forMonotonousTimestamps()
                        .withTimestampAssigner(
                                (element, recordTimestamp) -> element.modifiedTime.getTime()));

        SingleOutputStreamOperator<OrderDetail> orderDetailDataStream = tableEnv
                .toDataStream(
                        tableEnv.sqlQuery("select * from order_detail"),
                        OrderDetail.class)
                .assignTimestampsAndWatermarks(WatermarkStrategy.
                        <OrderDetail>forMonotonousTimestamps()
                        .withTimestampAssigner(
                                (element, recordTimestamp) -> element.modifiedTime.getTime()));

        doTask(orderMainDataStream, orderDetailDataStream);
    }

    /**
     * 在表中定义Watermark
     */
    private static void withTableApi(StreamTableEnvironment tableEnv) {
        Table result = tableEnv.sqlQuery(
                        "select cast(R.order_sn as bigint) as sn,\n" +
                        "       R.order_money as order_price, \n" +
                        "       sum(product_cnt) as order_detail_count\n" +
                        "from (\n" +
                        "   select order_sn, \n" +
                        "          product_cnt, \n" +
                        "          window_start, \n" +
                        "          window_end \n" +
                        "   from table (\n" +
                        "       tumble(table order_detail, descriptor(modified_time), interval '5' second)\n" +
                        "   )\n" +
                        ") L\n" +
                        "join (\n" +
                        "   select order_sn, \n" +
                        "          order_money, \n" +
                        "          window_start, \n" +
                        "          window_end \n" +
                        "   from table (\n" +
                        "       tumble(table order_main, descriptor(modified_time) ,interval '5' second)\n" +
                        "   )\n" +
                        ") R\n" +
                        "on L.order_sn = R.order_sn and L.window_start = R.window_start and L.window_end = R.window_end\n" +
                        "group by R.window_start, R.window_end, R.order_sn, R.order_money");

        tableEnv
                .toDataStream(result, Result.class)
                .addSink(getClickhouseSink());
    }

    /**
     * DataStream中的执行逻辑
     */
    private static void doTask(DataStream<OrderMain> orderMainDataStream,
                               DataStream<OrderDetail> orderDetailDataStream) {
        orderMainDataStream
                .coGroup(orderDetailDataStream)
                // 第一条流的order_sn
                .where(od -> od.orderSn)
                // 第二条流的order_sn
                .equalTo(om -> om.orderSn)
                .window(TumblingEventTimeWindows.of(Time.seconds(5)))
                .apply(
                        (oms, ods, out) -> {
                            for (OrderMain om : oms) {
                                int cnt = 0;
                                // 是否join上
                                boolean isJoined = false;
                                for (OrderDetail od : ods) {
                                    isJoined = true;
                                    cnt += od.productCnt;
                                }
                                if (isJoined) {
                                    out.collect(new Result(Long.parseLong(om.orderSn), om.orderMoney, cnt));
                                }
                            }
                        }, Types.POJO(Result.class))
                .addSink(getClickhouseSink());
    }

    /**
     * DataStream获取kafka源
     */
    private static KafkaSource<String> getKafkaSource(String topic, String group) {
        return KafkaSource.<String>builder()
                .setBootstrapServers(Consts.KAFKA_BOOTSTRAP_SERVER)
                .setTopics(topic)
                .setGroupId(group)
                .setProperty("auto.offset.reset", "latest")
                .setProperty("enable.auto.commit", "true")
                .setStartingOffsets(OffsetsInitializer.latest())
                // 可以自定义反序列方式为JSON
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();
    }

    private static SinkFunction<Result> getClickhouseSink() {
        return JdbcSink.sink(
                // 为保证幂等性，需要创建主键表，再insert into person values(?, ?) duplicated key update name = ?, age = ?
                // 或upsert into person values (?, ?)
                "insert into orderpositiveaggr(sn, orderprice, orderdetailcount) values(?, ?, ?)",
                // 使用preparedStatement设定参数
                new JdbcStatementBuilder<Result>() {
                    @Override
                    public void accept(PreparedStatement ps, Result result) throws SQLException {
                        ps.setLong(1, result.sn);
                        ps.setDouble(2, result.orderPrice);
                        ps.setInt(3, result.orderDetailCount);
                    }
                },
                // JDBC execution options
                JdbcExecutionOptions.builder()
                        // optional: default = 0, meaning no time-based execution is done
                        .withBatchSize(1000)
                        // optional: default = 5000 values
                        .withBatchIntervalMs(200)
                        // optional: default = 3
                        .withMaxRetries(5)
                        .build(),
                // JDBC connection parameters
                new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                        .withDriverName(Consts.CLICKHOUSE_DRIVER)
                        .withUrl(Consts.CLICKHOUSE_URL)
                        .withUsername(Consts.CLICKHOUSE_USERNAME)
                        .withPassword(Consts.CLICKHOUSE_PASSWORD)
                        .build());
    }
}
