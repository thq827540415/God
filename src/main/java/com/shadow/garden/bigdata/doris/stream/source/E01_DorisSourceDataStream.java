package com.shadow.garden.bigdata.doris.stream.source;

import com.shadow.garden.bigdata.util.FlinkEnvUtils;
import com.shadow.garden.bigdata.consts.DorisConsts;
import org.apache.doris.flink.cfg.DorisOptions;
import org.apache.doris.flink.cfg.DorisReadOptions;
import org.apache.doris.flink.deserialization.SimpleListDeserializationSchema;
import org.apache.doris.flink.source.DorisSource;
import org.apache.doris.flink.source.DorisSourceBuilder;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.List;

public class E01_DorisSourceDataStream {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();

        DorisOptions dorisOptions = DorisOptions.builder()
                .setFenodes(DorisConsts.FE_STR)
                .setUsername(DorisConsts.USERNAME)
                .setPassword(DorisConsts.PASSWORD)
                .setTableIdentifier("example_db.table1")
                .build();

        DorisSource<List<?>> dorisSource = DorisSourceBuilder.<List<?>>builder()
                .setDorisReadOptions(DorisReadOptions.builder().build())
                .setDorisOptions(dorisOptions)
                .setDeserializer(new SimpleListDeserializationSchema())
                .build();

        env.fromSource(dorisSource, WatermarkStrategy.noWatermarks(), "doris source")
                .print();

        env.execute(E01_DorisSourceDataStream.class.getSimpleName());
    }
}
