package com.ava.util;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

/**
 * @Author lancer
 * @Date 2022/6/7 15:38
 * @Description Environment Tools
 */
public class FlinkEnvUtils {

    static {
        System.setProperty("HADOOP_USER_NAME", "root");
    }

    /**
     * 设置全局算子的默认并行度 <= maxParallelism
     */
    private static final int GLOBAL_OPERATOR_PARALLELISM = 1;

    /**
     * 单个算子的最大并行度
     * the number of key groups，如果job恢复前后maxParallelism不一致，则不能恢复
     * <p>
     * KeyGroupRangeAssignment#computeDefaultMaxParallelism(int) -> default value：127 ~ 32768
     */
    private static final int GLOBAL_MAX_PARALLELISM = 4;

    private static final boolean WITH_WEB_UI = true;

    /**
     * 生成DataStream环境
     */
    public static StreamExecutionEnvironment getDSEnv() {
        if (WITH_WEB_UI) {
            return StreamExecutionEnvironment
                    .createLocalEnvironmentWithWebUI(new Configuration())
                    .setParallelism(GLOBAL_OPERATOR_PARALLELISM)
                    .setMaxParallelism(GLOBAL_MAX_PARALLELISM);
        }
        return StreamExecutionEnvironment
                .getExecutionEnvironment(new Configuration())
                .setParallelism(GLOBAL_OPERATOR_PARALLELISM)
                .setMaxParallelism(GLOBAL_MAX_PARALLELISM);
    }

    /**
     * 基于配置，生成DataStream环境
     */
    public static StreamExecutionEnvironment getDSEnv(Configuration conf) {
        if (WITH_WEB_UI) {
            return StreamExecutionEnvironment
                    .createLocalEnvironmentWithWebUI(conf)
                    .setParallelism(GLOBAL_OPERATOR_PARALLELISM)
                    .setMaxParallelism(GLOBAL_MAX_PARALLELISM);
        }
        return StreamExecutionEnvironment
                .getExecutionEnvironment(conf)
                .setParallelism(GLOBAL_OPERATOR_PARALLELISM)
                .setMaxParallelism(GLOBAL_MAX_PARALLELISM);
    }

    /**
     * 基于流环境，生成Table环境
     */
    public static StreamTableEnvironment getTableEnv(StreamExecutionEnvironment env) {
        return StreamTableEnvironment.create(env);
    }

    /**
     * 基于流环境和配置，生成Table环境
     */
    public static StreamTableEnvironment getTableEnv(
            StreamExecutionEnvironment env,
            EnvironmentSettings settings) {
        return StreamTableEnvironment.create(env, settings);
    }

    /**
     * 基于配置，生成Table环境
     * @param settings 指定当前表环境的执行模式(流-默认/批)和计划器planner(blink planner-默认)
     */
    public static TableEnvironment getTableEnv(EnvironmentSettings settings) {
        return TableEnvironment.create(settings);
    }
}
