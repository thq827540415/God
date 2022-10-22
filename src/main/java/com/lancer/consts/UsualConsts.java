package com.lancer.consts;

public class UsualConsts {
    public static final String NC_HOST = "bigdata01";

    public static final String HDFS_URL = "hdfs://bigdata01:9000";

    /**
     * 算子并行度
     * parallelism <= maxParallelism
     */
    public static final Integer GLOBAL_OPERATOR_PARALLELISM = 2;

    /**
     * the number of key groups，如果job恢复前后maxParallelism不一致，则不能恢复
     * <p>
     * KeyGroupRangeAssignment#computeDefaultMaxParallelism(int) -> default value：127 ~ 32768
     */
    public static final Integer GLOBAL_MAX_PARALLELISM = 4;

    public static final Boolean WITH_WEB_UI = true;
}
