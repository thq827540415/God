package com.lancer.flink.stream.transformation;

import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.streaming.runtime.operators.CheckpointCommitter;
import org.apache.flink.streaming.runtime.operators.GenericWriteAheadSink;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;

public class E09_GenericWriteAheadSink {

    static class Demo extends GenericWriteAheadSink<String> {
        public Demo(CheckpointCommitter committer, TypeSerializer<String> serializer, String jobID) throws Exception {
            super(committer, serializer, jobID);
        }

        @Override
        protected boolean sendValues(Iterable<String> values, long checkpointId, long timestamp) throws Exception {
            return false;
        }

        @Override
        public void setKeyContextElement(StreamRecord<String> record) throws Exception {
            super.setKeyContextElement(record);
        }
    }
}
