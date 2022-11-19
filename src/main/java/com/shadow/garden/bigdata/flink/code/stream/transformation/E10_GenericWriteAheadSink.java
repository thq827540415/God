package com.shadow.garden.bigdata.flink.code.stream.transformation;

import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.streaming.runtime.operators.CheckpointCommitter;
import org.apache.flink.streaming.runtime.operators.GenericWriteAheadSink;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;

/**
 * WAL并不能百分百提供Exactly-Once
 */
public class E10_GenericWriteAheadSink {

    /**
     * 由于没有实现SinkFunction接口，因此无法使用DataStream.addSink，得使用DataStream.transform()
     */
    static class StdOutWriteAheadSink extends GenericWriteAheadSink<String> {
        /**
         *
         * @param committer 来控制外部持久化系统存储和查找已提交的检查点信息
         * @param serializer 用于序列化输入记录
         * @param jobID 传递给CheckpointCommitter，用于应用重启后标识提交信息的任务ID
         */
        public StdOutWriteAheadSink(CheckpointCommitter committer, TypeSerializer<String> serializer, String jobID) throws Exception {

            super(committer, serializer, jobID);
        }

        /**
         * 调用该方法将已完成快照对应的记录写入外部存储系统
         *
         * @param values 改次快照对应的全部记录
         * @param checkpointId The checkpoint ID of the checkpoint to be written
         * @param timestamp 该次快照的生成时间
         * @return 全部记录写出成功时返回true，如果失败则返回false
         */
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
