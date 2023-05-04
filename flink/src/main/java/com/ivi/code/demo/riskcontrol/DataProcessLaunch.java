package com.ivi.code.demo.riskcontrol;

import com.ivi.code.util.FlinkEnvUtils;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class DataProcessLaunch {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = FlinkEnvUtils.getDSEnv();



        env.execute(DataProcessLaunch.class.getSimpleName());
    }
}
