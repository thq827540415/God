package com.shadow.garden.bigdata.flink.code.demo;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.logging.log4j.util.Strings;

import java.io.RandomAccessFile;

/**
 * 1.自定义并行的source
 * 2.使用operatorState记录偏移量
 */
public class MyAtLeastOnceSourceDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setParallelism(4);

        env.enableCheckpointing(30000);

        SingleOutputStreamOperator<String> socketSource = env
                .socketTextStream("bigdata01", 9999)
                .map(new MapFunction<String, String>() {
                    @Override
                    public String map(String value) throws Exception {
                        if ("error".equals(value.trim())) {
                            throw new RuntimeException("error test");
                        }
                        return value;
                    }
                });

        DataStreamSource<String> source = env.addSource(new MyAtLeastOnceSource("./Flink-DataStream/in"));

        source.union(socketSource).print();

        env.execute();
    }

    private static class MyAtLeastOnceSource extends RichParallelSourceFunction<String> implements CheckpointedFunction {

        private boolean running = true;
        private transient ListState<Long> listState;
        private Long offset = 0L;

        /**
         * 路径path如果用transient修饰，那么将不会被序列化发送到TM中
         */
        private final String path;

        public MyAtLeastOnceSource(String path) {
            this.path = path;
        }

        /**
         * 在初始化subtask的时候，会调用一次该方法，该方法是为了从stateBackend恢复状态数据
         * 比open方法先执行
         */
        @Override
        public void initializeState(FunctionInitializationContext context) throws Exception {
            // 恢复状态，恢复历史偏移量
            ListStateDescriptor<Long> listStateDescriptor = new ListStateDescriptor<>("offset-test", Long.class);
            // context.getKeyedStateStore()
            listState = context.getOperatorStateStore().getListState(listStateDescriptor);
            if (context.isRestored()) { // 判断是否已经恢复了，是否可读了，通俗来说，就是状态是否已经都放入listState中了，如果没有，那么下面代码不会执行
                for (Long historyOffset : listState.get()) {
                    offset = historyOffset;
                }
            }
        }

        /**
         * 在每一次checkpoint的时候，该Task的每个subtask都会调用一次该方法(开新线程)
         */
        @Override
        public void snapshotState(FunctionSnapshotContext context) throws Exception {
            listState.clear(); // 清除原来的偏移量
            listState.add(offset);
        }

        @Override
        public void run(SourceContext<String> ctx) throws Exception {
            // 可以从指定的偏移量读取数据
            RandomAccessFile accessFile = new RandomAccessFile(path + "/" + getRuntimeContext().getIndexOfThisSubtask() + ".txt", "r");
            // 定位到指定偏移量
            accessFile.seek(offset);

            // 监听文件是否有新内容写入
            while (running) {
                String line = accessFile.readLine();
                if (Strings.isNotBlank(line)) {
                    synchronized (ctx.getCheckpointLock()) { // flink帮我们实现的锁，当进行checkpoint的时候，如果这个锁由修改offset的线程占有，那么就会等到修改完offset，才进行checkpoint
                        ctx.collect(getRuntimeContext().getIndexOfThisSubtask() + ".txt --> " + line);
                        // 更新偏移量
                        offset = accessFile.getFilePointer();
                    }
                } else {
                    Thread.sleep(500);
                }
            }
            accessFile.close();
        }

        @Override
        public void cancel() {
            running = false;
        }
    }
}
