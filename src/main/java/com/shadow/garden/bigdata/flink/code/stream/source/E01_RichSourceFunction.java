package com.shadow.garden.bigdata.flink.code.stream.source;

import com.shadow.garden.bigdata.util.FlinkEnvUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;

/**
 * @Author lancer
 * @Date 2022/6/9 12:35
 * @Description 自定义RichSourceFunction
 *              boolean isParallel = function instanceof ParallelSourceFunction;
 */
public class E01_RichSourceFunction {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        env.setParallelism(2);

        env.addSource(new MyRichSourceFunction())
                .print();

        env.execute(E01_RichSourceFunction.class.getSimpleName());
    }

    /**
     * 自定义并行的RichSourceFunction
     */
    private static class MyParallelRichSourceFunction extends RichParallelSourceFunction<String> {
        private boolean running = true;

        /**
         * 每个并行度调用一次
         */
        @Override
        public void open(Configuration parameters) throws Exception {
            System.out.println("===== open =====");
        }

        @Override
        public void close() throws Exception {
            System.out.println("===== close =====");
            super.close();
        }

        @Override
        public void run(SourceContext<String> ctx) throws Exception {
            while (running) {
                int indexOfThisSubtask = getRuntimeContext().getIndexOfThisSubtask();
                if (indexOfThisSubtask != 0) {
                    ctx.collect("2");
                } else {
                    ctx.collect("1");
                }
                Thread.sleep(1000);
            }
        }

        /**
         * 通过cancel方法控制run中的循环
         */
        @Override
        public void cancel() {
            running = false;
        }
    }


    /**
     * non-parallel的Source
     */
    private static class MyRichSourceFunction extends RichSourceFunction<String> {

        private boolean running = true;

        @Override
        public void open(Configuration parameters) throws Exception {
            System.out.println("===== open =====");
        }

        @Override
        public void close() throws Exception {
            System.out.println("===== close =====");
            super.close();
        }

        @Override
        public void run(SourceContext<String> ctx) throws Exception {
            while (running) {
                if (getRuntimeContext().getIndexOfThisSubtask() != 0) {
                    ctx.collect("2");
                } else {
                    ctx.collect("1");
                }
                Thread.sleep(1000);
            }
        }

        @Override
        public void cancel() {
            running = false;
        }
    }
}
