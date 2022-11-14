package com.lancer.bigdata.consts;

public class UsualConsts {
    public static final String NC_HOST = "localhost";

    public static final String HDFS_URL = "hdfs://bigdata01:9000";

    /**
     * 设置全局算子的默认并行度 <= maxParallelism
     */
    public static final Integer GLOBAL_OPERATOR_PARALLELISM = 1;

    /**
     * 单个算子的最大并行度
     * the number of key groups，如果job恢复前后maxParallelism不一致，则不能恢复
     * <p>
     * KeyGroupRangeAssignment#computeDefaultMaxParallelism(int) -> default value：127 ~ 32768
     */
    public static final Integer GLOBAL_MAX_PARALLELISM = 4;

    public static final Boolean WITH_WEB_UI = true;
}
