package com.shadow.garden.bigdata.flink.code.stream.transformation;

import com.shadow.garden.bigdata.consts.Consts;
import com.shadow.garden.bigdata.util.FlinkEnvUtils;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.CoGroupFunction;
import org.apache.flink.api.common.functions.JoinFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.ProcessJoinFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;

/**
 * 将一个窗口内的数据进行join
 */
public class E09_JoinAndIntervalJoin {

    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Order {
        public String orderId;
        public double totalAmount;
        public String orderStatus;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class OrderDetail {
        public String orderId;
        public String kind;
        public double price;
        public int nums;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Result {
        public String orderId;
        public String orderStatus;
        public String kind;
        public double price;
        public int nums;
    }

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();


        // 订单主表(订单id，订单总金额，订单状态)
        // 1000,o100,2000,done
        // 5000,o101,800,done
        SingleOutputStreamOperator<Order> order = env
                .socketTextStream(Consts.NC_HOST, 9998)
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy
                                .<String>forMonotonousTimestamps()
                                .withTimestampAssigner(new SerializableTimestampAssigner<String>() {
                                    @Override
                                    public long extractTimestamp(String element, long recordTimestamp) {
                                        return Long.parseLong(element.split(",")[0]);
                                    }
                                }))
                .map(
                        line -> {
                            String[] split = line.split(",");
                            return new Order(split[1], Double.parseDouble(split[2]), split[3]);
                        }, Types.POJO(Order.class));

        // 订单明细表(订单主表id，分类，单价，数量)
        // 1000,o100,phone,500,2
        // 2000,o100,pc,1000,1
        // 3000,o101,pad,800,1
        // 5000,o102,goods,1000,1
        SingleOutputStreamOperator<OrderDetail> orderDetail = env
                .socketTextStream(Consts.NC_HOST, 9999)
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy
                                .<String>forMonotonousTimestamps()
                                .withTimestampAssigner(new SerializableTimestampAssigner<String>() {
                                    @Override
                                    public long extractTimestamp(String element, long recordTimestamp) {
                                        return Long.parseLong(element.split(",")[0]);
                                    }
                                }))
                .map(
                        line -> {
                            String[] split = line.split(",");
                            return new OrderDetail(
                                    split[1],
                                    split[2],
                                    Double.parseDouble(split[3]),
                                    Integer.parseInt(split[4]));
                        })
                .returns(Types.POJO(OrderDetail.class));

        // o100,done,phone,500,2
        // o100,done,pc,1000,1
        // doJoin(order, orderDetail);

        // o100,done,phone,500,2
        // o100,done,pc,1000,1
        // o101,null,pad,800,1
        // doLeftJoin(order, orderDetail);

        // 只要有连接到的就触发
        // doIntervalJoin(order, orderDetail);

        env.execute(E09_JoinAndIntervalJoin.class.getSimpleName());
    }


    /**
     * 内连接
     * join底层采用union，和window、Watermark有关
     */
    private static void doJoin(DataStream<Order> order, DataStream<OrderDetail> orderDetail) {
        order
                // 底层是coGroup，coGroup底层是union；原理是KeyedStream + Window
                .join(orderDetail)
                // 第一条流的连接条件
                .where(o -> o.orderId)
                // 第二条流的连接条件
                .equalTo(od -> od.orderId)
                .window(TumblingEventTimeWindows.of(Time.seconds(5)))
                // .trigger()
                // .evictor()
                // 但是没有测流输出迟到数据
                // .allowedLateness()
                // 没有process方法，只有apply
                .apply(
                        new JoinFunction<Order, OrderDetail, Result>() {
                            @Override
                            public Result join(Order o, OrderDetail od) throws Exception {
                                return new Result(
                                        o.orderId,
                                        o.orderStatus,
                                        od.kind,
                                        od.price,
                                        od.nums);
                            }
                        })
                .print();
    }

    /**
     * 左/右连接需要自己手动去连接，采用更底层的算子coGroup
     */
    private static void doLeftJoin(DataStream<Order> order, DataStream<OrderDetail> orderDetail) {
        // orderDetail为左表，order为右表
        orderDetail
                .coGroup(order)
                .where(od -> od.orderId)
                .equalTo(o -> o.orderId)
                .window(TumblingEventTimeWindows.of(Time.seconds(5)))
                .apply(
                        new CoGroupFunction<OrderDetail, Order, Result>() {
                            @Override
                            public void coGroup(Iterable<OrderDetail> od,
                                                Iterable<Order> o,
                                                Collector<Result> out) throws Exception {
                                // 如果o有数据，od有数据，可以实现join
                                // 如果o有数据，od没有数据，可以实现left join
                                // 如果o没有数据，od有数据，可以实现right join

                                // 是否join上
                                boolean isJoined = false;
                                for (OrderDetail left : od) {
                                    // 只要有可以连接的对象，则
                                    for (Order right : o) {
                                        isJoined = true;
                                        out.collect(new Result(
                                                left.orderId,
                                                right.orderStatus,
                                                left.kind,
                                                left.price,
                                                left.nums
                                        ));
                                    }
                                    if (!isJoined) {
                                        out.collect(new Result(
                                                left.orderId,
                                                null,
                                                left.kind,
                                                left.price,
                                                left.nums
                                        ));
                                    }
                                }
                            }
                        })
                .print();
    }

    /**
     * intervalJoin底层采用connect，和window没有关系，和Watermark有关系。底层用定时器来触发计算
     * left和right各用一个MapState保存数据
     */
    private static void doIntervalJoin(DataStream<Order> order, DataStream<OrderDetail> orderDetail) {
        orderDetail
                .keyBy(od -> od.orderId)
                .intervalJoin(
                        order.keyBy(o -> o.orderId))
                // 前1s和后1s数据能join上
                // leftElement.timestamp + lowerBound <= rightElement.timestamp <= leftElement.timestamp + upperBound
                .between(Time.seconds(-1), Time.seconds(1))
                // [lower, upper)
                .upperBoundExclusive()
                // 只有process
                .process(
                        new ProcessJoinFunction<OrderDetail, Order, Result>() {
                            @Override
                            public void processElement(OrderDetail left,
                                                       Order right,
                                                       Context ctx,
                                                       Collector<Result> out) throws Exception {
                                out.collect(
                                        new Result(
                                                left.orderId,
                                                right.orderStatus,
                                                left.kind,
                                                left.price,
                                                left.nums));
                            }
                        })
                .print();
    }
}
