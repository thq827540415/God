package com.lancer.flink.stream.transformation;

import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.functions.sink.TwoPhaseCommitSinkFunction;

public class E08_TwoPhaseCommitSinkFunction {


    /**
     * 外部系统不但要支持事务，同时也要能支持根据事务ID去恢复之前的事务
     */
    static class Demo extends TwoPhaseCommitSinkFunction<Tuple2<String, Double>, String, Void> {
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
        public Demo(TypeSerializer<String> transactionSerializer, TypeSerializer<Void> contextSerializer) {
            super(transactionSerializer, contextSerializer);
        }

        @Override
        protected void invoke(String transaction, Tuple2<String, Double> value, Context context) throws Exception {

        }

        @Override
        protected String beginTransaction() throws Exception {
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
