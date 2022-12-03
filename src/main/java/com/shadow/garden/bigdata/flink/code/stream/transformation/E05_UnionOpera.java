package com.shadow.garden.bigdata.flink.code.stream.transformation;

import com.shadow.garden.bigdata.consts.Consts;
import com.shadow.garden.bigdata.util.FlinkEnvUtils;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @Author lancer
 * @Date 2022/6/7 18:22
 * @Description 使用union算子将多条类型相同的流合并
 */
public class E05_UnionOpera {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        DataStreamSource<String> s1 = env.socketTextStream(Consts.NC_HOST, 9998);
        DataStreamSource<String> s2 = env.socketTextStream(Consts.NC_HOST, 9999);

        // s1和s2的类型必须一样，多流合并，直接将流合在一起，之后操作都是在合并的这条流上
        DataStream<String> unionStream = s1.union(s2);

        // 在合并的那条流上操作
        unionStream.map(p -> p)
                .print();

        env.execute(E05_UnionOpera.class.getSimpleName());
    }
}
