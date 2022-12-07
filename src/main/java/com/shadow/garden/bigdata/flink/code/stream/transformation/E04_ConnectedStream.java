package com.shadow.garden.bigdata.flink.code.stream.transformation;

import com.solitude.consts.Consts;
import com.solitude.util.FlinkEnvUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.flink.streaming.api.datastream.ConnectedStreams;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.CoFlatMapFunction;
import org.apache.flink.util.Collector;

/**
 * @Author lancer
 * @Date 2022/6/7 18:26
 * @Description 使用connect算子将任意两流进行连接，可以连接广播流；两条流共享state
 */
public class E04_ConnectedStream {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        DataStreamSource<String> s1 = env.socketTextStream(Consts.NC_HOST, 9998);

        SingleOutputStreamOperator<Integer> s2 = env.socketTextStream(Consts.NC_HOST, 9999)
                .filter(NumberUtils::isNumber)
                .map(Integer::parseInt);


        ConnectedStreams<String, Integer> connectedStreams = s1.connect(s2);

        // 同理CoMpaFunction：ConnectedStreams -> DataStream
        SingleOutputStreamOperator<String> res = connectedStreams
                .flatMap(
                        new CoFlatMapFunction<String, Integer, String>() {
                            @Override
                            public void flatMap1(String value, Collector<String> out) throws Exception {
                                out.collect(value.toUpperCase());
                            }

                            @Override
                            public void flatMap2(Integer value, Collector<String> out) throws Exception {
                                out.collect(String.valueOf(value * 10));
                            }
                        });

        res.print();

        env.execute(E04_ConnectedStream.class.getSimpleName());
    }
}
