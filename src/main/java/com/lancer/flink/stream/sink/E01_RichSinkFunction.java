package com.lancer.flink.stream.sink;

import com.lancer.utils.FlinkEnvUtils;
import com.lancer.consts.UsualConsts;
import org.apache.flink.api.common.functions.util.PrintSinkOutputWriter;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;

/**
 * @Author lancer
 * @Date 2022/6/15 18:54
 * @Description print算子的实现方式
 */
public class E01_RichSinkFunction {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        DataStreamSource<String> source = env.socketTextStream(UsualConsts.NC_HOST, 9999);

        source.addSink(new Print());

        env.execute(E01_RichSinkFunction.class.getSimpleName());
    }

    private static class Print extends RichSinkFunction<String> {
        private final PrintSinkOutputWriter<String> writer = new PrintSinkOutputWriter<>(false);

        @Override
        public void open(Configuration parameters) throws Exception {
            // StreamingRuntimeContext context = (StreamingRuntimeContext) getRuntimeContext();
            writer.open(getRuntimeContext().getIndexOfThisSubtask(), getRuntimeContext().getNumberOfParallelSubtasks());
        }

        @Override
        public void invoke(String value, Context context) throws Exception {
            writer.write(value);
        }
    }
}
