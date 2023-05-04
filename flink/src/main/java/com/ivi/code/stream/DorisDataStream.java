package com.ivi.code.stream;

import com.ivi.code.util.FlinkEnvUtils;
import org.apache.doris.flink.cfg.DorisExecutionOptions;
import org.apache.doris.flink.cfg.DorisOptions;
import org.apache.doris.flink.cfg.DorisReadOptions;
import org.apache.doris.flink.deserialization.SimpleListDeserializationSchema;
import org.apache.doris.flink.sink.DorisSink;
import org.apache.doris.flink.sink.writer.SimpleStringSerializer;
import org.apache.doris.flink.source.DorisSource;
import org.apache.doris.flink.source.DorisSourceBuilder;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

import java.util.List;

public class DorisDataStream {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        DorisSource<List<?>> dorisSource = DorisSourceBuilder.<List<?>>builder()
                .setDorisReadOptions(DorisReadOptions.builder().build())
                .setDorisOptions(
                        DorisOptions.builder()
                                .setFenodes("bigdata03:8030")
                                .setUsername("root")
                                .setPassword("123456")
                                .setTableIdentifier("example_db.table1")
                                .build())
                .setDeserializer(new SimpleListDeserializationSchema())
                .build();

        env.fromSource(dorisSource, WatermarkStrategy.noWatermarks(), "doris source")
                .flatMap(new FlatMapFunction<List<?>, String>() {
                    @Override
                    public void flatMap(List<?> value, Collector<String> out) throws Exception {
                        value.forEach(str -> out.collect((String) str));
                    }
                })
                .sinkTo(getSinkByString());

        env.execute(DorisDataStream.class.getSimpleName());
    }

    /**
     * 数据以String形式存储
     */
    static DorisSink<String> getSinkByString() {
        return DorisSink.<String>builder()
                .setDorisReadOptions(DorisReadOptions.builder().build())
                .setDorisOptions(
                        DorisOptions.builder()
                                .setFenodes("bigdata03:8030")
                                .setUsername("root")
                                .setPassword("123456")
                                .setTableIdentifier("example_db.table1")
                                .build())
                .setDorisExecutionOptions(
                        DorisExecutionOptions.builder()
                                //_stream_load label prefix
                                .setLabelPrefix("label-doris")
                                .build())
                .setSerializer(new SimpleStringSerializer())
                .build();
    }

    /**
     * 数据以RowData形式存储
     */
    static DorisSink<String> getSinkByRowData() {
        return null;
    }
}
