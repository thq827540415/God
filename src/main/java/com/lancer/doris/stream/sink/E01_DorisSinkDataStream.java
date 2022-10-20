package com.lancer.doris.stream.sink;

import com.lancer.FlinkEnvUtils;
import com.lancer.consts.DorisConsts;
import org.apache.doris.flink.cfg.DorisExecutionOptions;
import org.apache.doris.flink.cfg.DorisOptions;
import org.apache.doris.flink.cfg.DorisReadOptions;
import org.apache.doris.flink.sink.DorisCommittable;
import org.apache.doris.flink.sink.DorisSink;
import org.apache.doris.flink.sink.writer.DorisWriterState;
import org.apache.doris.flink.sink.writer.SimpleStringSerializer;
import org.apache.flink.api.connector.sink.Sink;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class E01_DorisSinkDataStream {
    public static void main(String[] args) {

        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        DataStreamSource<Tuple2<String, Integer>> source = env.fromElements(new Tuple2<>("doris", 1));

        DorisSink<String> dorisSink = generateSinkByString();

        source.map(t -> t.f0 + "\t" + t.f1)
                .sinkTo(dorisSink);
    }

    /**
     * 数据以String形式存储
     */
    private static DorisSink<String> generateSinkByString() {
        DorisOptions dorisOptions = DorisOptions.builder()
                .setFenodes(DorisConsts.DORIS_FE_STR)
                .setUsername(DorisConsts.DORIS_USERNAME)
                .setPassword(DorisConsts.DORIS_PASSWORD)
                .setTableIdentifier("example_db.table1")
                .build();

        DorisExecutionOptions dorisExecutionOptions = DorisExecutionOptions.builder()
                //_stream_load label prefix
                .setLabelPrefix("label-doris")
                .build();

        return DorisSink.<String>builder()
                .setDorisReadOptions(DorisReadOptions.builder().build())
                .setDorisOptions(dorisOptions)
                .setDorisExecutionOptions(dorisExecutionOptions)
                .setSerializer(new SimpleStringSerializer())
                .build();
    }

    /**
     * 数据以RowData形式存储
     */
    private static DorisSink<String> generateSinkByRowData() {
        return null;
    }
}