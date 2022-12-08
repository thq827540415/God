package com.solitude.util;

import com.solitude.consts.Consts;
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

    private FlinkEnvUtils(){
    }

    static {
        System.setProperty("HADOOP_USER_NAME", "root");
    }

    /**
     * 生成DataStream环境
     */
    public static StreamExecutionEnvironment getDSEnv() {
        if (Consts.FLINK_WITH_WEB_UI) {
            return StreamExecutionEnvironment
                    .createLocalEnvironmentWithWebUI(new Configuration())
                    .setParallelism(Consts.FLINK_GLOBAL_OPERATOR_PARALLELISM)
                    .setMaxParallelism(Consts.FLIN_GLOBAL_MAX_PARALLELISM);
        }
        return StreamExecutionEnvironment
                .getExecutionEnvironment(new Configuration())
                .setParallelism(Consts.FLINK_GLOBAL_OPERATOR_PARALLELISM)
                .setMaxParallelism(Consts.FLIN_GLOBAL_MAX_PARALLELISM);
    }

    /**
     * 基于配置，生成DataStream环境
     */
    public static StreamExecutionEnvironment getDSEnv(Configuration conf) {
        if (Consts.FLINK_WITH_WEB_UI) {
            return StreamExecutionEnvironment
                    .createLocalEnvironmentWithWebUI(conf)
                    .setParallelism(Consts.FLINK_GLOBAL_OPERATOR_PARALLELISM)
                    .setMaxParallelism(Consts.FLIN_GLOBAL_MAX_PARALLELISM);
        }
        return StreamExecutionEnvironment
                .getExecutionEnvironment(conf)
                .setParallelism(Consts.FLINK_GLOBAL_OPERATOR_PARALLELISM)
                .setMaxParallelism(Consts.FLIN_GLOBAL_MAX_PARALLELISM);
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
