package com.ivi.code.stream;

import com.ivi.code.util.FlinkEnvUtils;
import org.apache.commons.io.FileUtils;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CacheFile {
    public static void main(String[] args) {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();
        env.registerCachedFile("", "distributedCache");
        env.fromElements("1", "2", "3")
                .map(new RichMapFunction<String, String>() {
                    transient List<String> data = new ArrayList<>();

                    @Override
                    public void open(Configuration parameters) throws Exception {
                        File file = getRuntimeContext()
                                .getDistributedCache()
                                .getFile("distributedCache");

                        data.addAll(FileUtils.readLines(file, "UTF-8"));
                    }

                    @Override
                    public String map(String value) throws Exception {
                        return null;
                    }
                });
    }
}
