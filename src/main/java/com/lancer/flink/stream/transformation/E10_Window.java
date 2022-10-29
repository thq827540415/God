package com.lancer.flink.stream.transformation;

import com.lancer.FlinkEnvUtils;
import com.lancer.consts.UsualConsts;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.time.Duration;


/**
 * window主要两种方式(window -> WindowedStream)和(windowAll -> AllWindowedStream)
 * 1. windowAll ->
 * 底层将其任何输入的流都定义成了一个key值恒为0的KeyedStream（keyBy(t -> 0)），所有数据均发往KeyGroup0，
 * 之后的操作全部都作用在KeyGroup0上，即该KeyGroup0对应的CopyOnWriteStateMap上
 * 2. window ->
 * <p>
 * window四大组件 WindowAssigner、evictor、trigger、window process
 * 1. WindowAssigner
 * （1）GlobalWindows
 * （2）通过Gap划分窗口的：父类 -> MergingWindowAssigner
 * i.XXXTimeSessionWindows
 * ii.DynamicXXXSessionWindows
 * （3）滑动窗口：SlidingXXXWindows
 * （4）滚动窗口：TumblingXXXWindows
 * 2. Evictor：每种WindowAssigner对应着一个默认的Evictor -> windowAssigner.getDefaultTrigger
 * （1）CountEvictor
 * （2）DeltaEvictor
 * （3）TimeEvictor
 */
public class E10_Window {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        DataStreamSource<String> source = env.socketTextStream(UsualConsts.NC_HOST, 9999);

        doTimeWindow(source);

        env.execute(E10_Window.class.getSimpleName());
    }


    /**
     * countWindowAll
     * 底层将其定义成了一个KeyedStream，但是其key值恒为0 keyBy(t -> 0)，所有数据均发往KeyGroup0，
     * 所以其状态也保存在HeapKeyedStateBackend中，
     * 1. 单参：将其封装为KeyedStream -> this.input = input.keyBy(new NullByteKeySelector())
     * 2. 双参：定义了一个名为window-contents的ListState，其余的跟KeyedStream中的countWindow方法一样
     */
    private static void doCountWindowAll(DataStreamSource<String> source) {
        source
                .map(Integer::parseInt)
                // tigger：2 -> 每两条数据触发一次窗口；evictor：5 -> 最多能容纳5条数据
                .countWindowAll(5, 2)
                .sum(0)
                .print();
    }

    /**
     * countWindow
     * 其中的状态是在KeyedStateBackend中创建，既可以用countWindow，也可以用countWindowAll
     * 1. 单参：底层用名为window-contents的ReducingState保存，底层trigger采用的使用PurgingTrigger，
     * 启动有个名为count的状态，注册在KeyedStateBackend中，其中的onElement方法，每来一条数据，状态就+1，
     * 判断是否超过窗口最大值，当触发窗口计算后，会将count状态清空，同时将窗口状态清空
     * 2. 双参：底层用名为window-contents的ListState保存，trigger编程CountTrigger，触发窗口后，
     * 用CountEvictor去掉数据，然后执行计算，然后清空窗口状态（window-contends），再将evict后的数据放入窗口状态中
     */
    private static void doCountWindow(DataStreamSource<String> source) {
        source
                .map(line -> Tuple2.of(line, 1), Types.TUPLE(Types.STRING, Types.INT))
                .keyBy(t -> t.f0)
                .countWindow(5)
                .sum(1)
                .print();
    }

    /**
     * 1000, zs
     * 4999, zs
     * 5000, zs -> 触发窗口，但是不参加窗口计算
     */
    private static void doTimeWindow(DataStreamSource<String> source) {
        source

                .assignTimestampsAndWatermarks(
                        // 发送水位：TimestampsAndWatermarksOperator
                        // 获取传过来的水位：StreamTaskNetworkInput
                        // BoundedOutOfOrdernessWatermarks中的onPeriodicEmit会周期性调用，往下游发送Watermark
                        // new Watermark(maxTimestamp - outOfOrdernessMillis - 1)
                        // recordWriter.broadcastEmit(serializationDelegate) -> 通过广播的方式发送WM（不同subtask之间）
                        // onEvent方法会根据数据来更新最大时间戳
                        // eventtime.Watermark只是用来判断是否需要发送wm，watermark.Watermark才是真正发送的数据，继承StreamElement
                        // 选择输入的Watermark中比较小的那个，将其与当前Watermark进行对比，如果大则通过processWatermark发送到下游，
                        // 从而保证了全局Watermark的最小性
                        WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofSeconds(0))
                                .withTimestampAssigner(new SerializableTimestampAssigner<String>() {
                                    @Override
                                    public long extractTimestamp(String element, long recordTimestamp) {
                                        return Long.parseLong(element.split(",")[0]);
                                    }
                                }))
                                // 如果超过1分钟还没有收到数据，则时间会自主往前推进
                                // .withIdleness(Duration.ofSeconds(60)))

                .map(
                        line -> {
                            String[] split = line.split(",");
                            // (zs, 1)
                            return Tuple2.of(split[1], 1);
                        }, Types.TUPLE(Types.STRING, Types.INT))
                .keyBy(t -> t.f0)
                // 默认计算窗口大小: TimeWindow.getWindowStartWithOffset
                // ⭐(timestamp - (timestamp + (offset -> 0) +  windowSize) % windowSize, windowSize)
                // 当时间戳为正数，且不超过窗口大小时，窗口默认大小为 -> [0, windowSize)
                // ⭐窗口触发时间（在Trigger中）：(window.maxTimestamp() -> end - 1) <= ctx.getCurrentWatermark()
                // 4999 <= 4999 -> 故当watermark即后者到达4999的时候，触发计算，即当数据中的EventTime到达5000时计算
                // 若加上允许延迟时间，实质上就是降低Watermark，让窗口晚点触发
                // 窗口触发后，数据会一直保存在状态中，但是不会再计算
                // .window(EventTimeSessionWindows.withGap())
                // .window(SlidingEventTimeWindows.of(Time.seconds(5), Time.seconds(3)))
                .window(TumblingEventTimeWindows.of(Time.seconds(5)))
                // 迟到数据，即窗口触发后，才过来的数据，而Watermark是用于处理乱序数据
                // 当数据迟到时，会先计算该数据所在的窗口是否已经过去了(window.maxTimestamp + allowedLateness <= watermark)，
                // 若是，则该数据不会放入State中，即窗口触发后，allowedLateness时间内还能再次触发窗口
                // 此时，当数据的时间 <= currentWatermark + allowedLateness，则会对该条记录进行处理
                // 若定义了测流标签，则该迟到数据会放入测流中
                // 若没定义测流，则将通过一个SimpleCounter，将其通过名为numLateRecordsDropped的度量metric显示出来
                // .allowedLateness(Time.seconds(1))
                // .sideOutputLateData()
                .sum(1)
                .print();
    }
}
