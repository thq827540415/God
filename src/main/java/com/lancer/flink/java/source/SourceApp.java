package com.lancer.flink.java.source;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;

import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;


import org.apache.flink.util.Collector;
import org.apache.flink.util.NumberSequenceIterator;

import java.util.ArrayList;
import java.util.List;


/**
 * 并行度通过ExecutionConfig类来设定
 *
 * 通过run方法Starts the source ---> 源操作（一个并行度执行一次）
 *
 * 通过open方法，在每个Function使用前做操作  ---> 一个并行度执行一次
 */
public class SourceApp {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.execute("SourceApp");
    }


    /*
     * 通过读取ExecutionConfig中的parallelism，通过FileInputFormat读取文件，按parallelism数逻辑切分?
     * */
    public static void test04(StreamExecutionEnvironment env) {
        DataStreamSource<String> source = env.readTextFile("./data/access.log");
        System.out.println(source.getParallelism()); // 16  ?
    }


    // fromParallelCollection
    public static void test02(StreamExecutionEnvironment env) {
        // 调用AbstractRichFunction的open方法，从RuntimeContext中获取task数量（并行度），然后返回一个切分后的迭代器iterator，通过ExecutionConfig中的parallelism来设定调用次数
        /*
        * FromSplittableIteratorFunction中的opne方法：
        * public void open(Configuration parameters) throws Exception {
                int numberOfSubTasks = this.getRuntimeContext().getNumberOfParallelSubtasks(); // 获取task的数量
                int indexofThisSubTask = this.getRuntimeContext().getIndexOfThisSubtask();  // 获取该task的下标
                this.iterator = this.fullIterator.split(numberOfSubTasks)[indexofThisSubTask]; // 返回迭代器数组中，task下标处的迭代器
                this.isRunning = true;
         }
        * */
        DataStreamSource<Long> source = env.fromParallelCollection(new NumberSequenceIterator(1, 10), Long.class);
        System.out.println("fromParallelCollection's parallelism：" + source.getParallelism()); // 16

        SingleOutputStreamOperator<Long> filterStream = source.filter(s -> s >= 5);

        System.out.println("filterStream's parallelism：" + filterStream.getParallelism());
    }
}
