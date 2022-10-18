package com.lancer.flink.stream.transformation;

import com.lancer.FlinkEnvUtils;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @Author lancer
 * @Date 2022/6/7 17:43
 * @Description RichFunction的用法
 */
public class E01_RichOperaFunction {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        DataStreamSource<String> source = env.socketTextStream("localhost", 9999);

        SingleOutputStreamOperator<String> res = source
                .map(new StringMapFunction()).setParallelism(2);

        res
                .print();

        env.execute(E01_RichOperaFunction.class.getSimpleName());
    }

    /**
     * rich的类，具有生命周期的方法（open，close，getRuntimeContext），继承自AbstractRichFunction
     * open方法：Initialization method for the function，调用该Function用几个并行度，就调用open方法几次
     * 设置并行度，实质上是对调用open、close方法的限制
     */
    private static class StringMapFunction extends RichMapFunction<String, String> {

        /**
         * 重写了获取上下文的方法
         */
        @Override
        public RuntimeContext getRuntimeContext() {
            return super.getRuntimeContext();
        }

        /**
         * 初始化操作，每个并行度都要执行
         */
        @Override
        public void open(Configuration parameters) throws Exception {
            System.out.println("~~~~~~~open~~~~~~~");
        }

        /**
         * 关闭操作，每个并行度都要执行
         */
        @Override
        public void close() throws Exception {
            System.out.println("~~~~~~~close~~~~~~~");
            super.close();
        }

        /**
         * 每条数据执行一次，跟并行度无关
         */
        @Override
        public String map(String value) throws Exception {
            System.out.println("~~~~~~~map~~~~~~~");
            return value;
        }
    }
}
