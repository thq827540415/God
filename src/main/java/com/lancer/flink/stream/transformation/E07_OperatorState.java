package com.lancer.flink.stream.transformation;

import org.apache.flink.api.common.functions.util.PrintSinkOutputWriter;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * OperateStream中同一个subTask中共享一个状态
 * OperatorState: 实现CheckpointedFunction, 在initializeState方法中初始化状态；Kafka Connector就是使用了OperatorState
 *      1. ListState
 *      2. BroadcastState
 *          (1) it has a map format.
 *          (2) an operator can have multiple broadcast states with different names.
 *          (3) Broadcast state is kept in-memory at runtime.
 */
public class E07_OperatorState {
    public static void main(String[] args) {

    }

    private static class MyState
            extends RichSinkFunction<Tuple2<String, Integer>>
            implements CheckpointedFunction {

        private transient ListState<Integer> listState;
        private final PrintSinkOutputWriter<Tuple2<String, Integer>> writer = new PrintSinkOutputWriter<>(false);

        @Override
        public void open(Configuration parameters) throws Exception {
            writer.open(getRuntimeContext().getIndexOfThisSubtask(), getRuntimeContext().getNumberOfParallelSubtasks());
        }

        @Override
        public void invoke(Tuple2<String, Integer> value, Context context) throws Exception {
            List<Integer> integers = (List<Integer>) listState.get();
            if (integers.size() == 0) {
                integers = new ArrayList<>(1);
                integers.add(0, value.f1);
            } else {
                Integer currentValue = integers.get(0);
                value.f1 += currentValue;
                integers.add(0, value.f1);
            }
            listState.update(integers);
            writer.write(value);
        }

        @Override
        public void snapshotState(FunctionSnapshotContext context) throws Exception {

        }

        @Override
        public void initializeState(FunctionInitializationContext context) throws Exception {
            ListStateDescriptor<Integer> listStateDescriptor = new ListStateDescriptor<>(
                    "list state descriptor",
                    Types.INT);
            listState = context.getOperatorStateStore().getListState(listStateDescriptor);
        }
    }

    private static void test(DataStreamSource<String> source) {
        source
                .map(
                        line -> {
                            String[] split = line.split("\\s+");
                            return Tuple2.of(split[0], Integer.parseInt(split[1]));
                        }, Types.TUPLE(Types.STRING, Types.INT))
                .addSink(new MyState());
    }
}
