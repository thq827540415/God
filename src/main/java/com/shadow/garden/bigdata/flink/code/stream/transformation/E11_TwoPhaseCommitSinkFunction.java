package com.shadow.garden.bigdata.flink.code.stream.transformation;

import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.functions.sink.TwoPhaseCommitSinkFunction;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class E11_TwoPhaseCommitSinkFunction {

    /**
     * 外部系统不但要支持事务，同时也要能支持根据事务ID去恢复之前的事务
     */
    static class TransactionalFileSink extends TwoPhaseCommitSinkFunction<Tuple2<String, Double>, String, Void> {
        /**
         * Use default {@link ListStateDescriptor} for internal state serialization. Helpful utilities
         * for using this constructor are {@link TypeInformation#of(Class)}, {@link
         * TypeHint} and {@link TypeInformation#of(TypeHint)}.
         * Example:
         *
         * <pre>{@code
         * TwoPhaseCommitSinkFunction(TypeInformation.of(new TypeHint<State<TXN, CONTEXT>>() {}));
         * }</pre>
         *
         * @param transactionSerializer {@link TypeSerializer} for the transaction type of this sink
         * @param contextSerializer     {@link TypeSerializer} for the context type of this sink
         */
        public TransactionalFileSink(TypeSerializer<String> transactionSerializer, TypeSerializer<Void> contextSerializer) {
            super(transactionSerializer, contextSerializer);
        }


        @Override
        protected void invoke(String transaction, Tuple2<String, Double> value, Context context) throws Exception {

        }

        @Override
        protected String beginTransaction() throws Exception {
            //  事务文件的路径有当前时间和任务索引决定
            String timeNow = LocalDateTime.now(ZoneId.of("UTC"))
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            int taskIdx = getRuntimeContext().getIndexOfThisSubtask();
            String transactionFile = timeNow + "-" + taskIdx;

            // 创建事务文件以及写入器
            // Files.createFile(Paths.get(tempPath))

            return null;
        }

        @Override
        protected void preCommit(String transaction) throws Exception {

        }

        @Override
        protected void commit(String transaction) {

        }

        @Override
        protected void abort(String transaction) {

        }
    }
}
