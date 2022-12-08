package com.solitude.bigdata.hadoop.code;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

@Slf4j
public class MRWordCount {

    static {
        System.setProperty("HADOOP_USER_NAME", "root");
    }

    @NoArgsConstructor
    @AllArgsConstructor
    private static class Bean implements WritableComparable<Bean> {
        private String word;
        private int count;

        /**
         * 用于排序，默认会被排序比较器和分区比较器使用
         */
        @Override
        public int compareTo(@NotNull MRWordCount.Bean o) {
            log.info("compareTo被调用==================================");
            return Integer.compare(0, o.count - this.count);
        }

        @Override
        public void write(DataOutput dataOutput) throws IOException {
            dataOutput.writeUTF(word);
            dataOutput.writeInt(count);
        }

        @Override
        public void readFields(DataInput dataInput) throws IOException {
            this.word = dataInput.readUTF();
            this.count = dataInput.readInt();
        }

        @Override
        public int hashCode() {
            return this.word.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj != null) {
                if (obj instanceof Bean) {
                    Bean o = (Bean) obj;
                    return o.hashCode() == this.hashCode();
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return this.word + "\t" + this.count;
        }
    }

    private static class MyMapper extends Mapper<LongWritable, Text, Bean, NullWritable> {
        @Override
        protected void setup(Mapper<LongWritable, Text, Bean, NullWritable>.Context context) throws IOException {
            // 获取缓存文件
            URI[] cacheFiles = context.getCacheFiles();

            // 获取该MapTask读取的数据块所在文件的文件名
            FileSplit fileSplit = (FileSplit) context.getInputSplit();
            fileSplit.getPath().getName();
        }

        @Override
        protected void map(LongWritable key, Text value,
                           Context context) throws IOException, InterruptedException {
            Arrays.stream(value.toString().split("\\s+"))
                    .forEach(word -> {
                        try {
                            Bean bean = new Bean(word, 1);
                            context.write(bean, NullWritable.get());
                        } catch (IOException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    private static class MyReducer extends Reducer<Bean, NullWritable, Bean, NullWritable> {

        /**
         * @param key 分组中第一个key
         */
        @Override
        protected void reduce(Bean key, Iterable<NullWritable> values,
                              Context context) throws IOException, InterruptedException {
            for (NullWritable ignored : values) {
                context.write(key, NullWritable.get());
            }
        }
    }

    private static class MyPartitioner extends Partitioner<Bean, NullWritable> {
        @Override
        public int getPartition(Bean bean, NullWritable nullWritable, int numPartitions) {
            return (bean.hashCode() & Integer.MAX_VALUE) % numPartitions;
        }
    }

    private static class MySortComparator extends WritableComparator {

        public MySortComparator() {
            super(Bean.class, true);
        }

        @Override
        public int compare(WritableComparable a, WritableComparable b) {
            return 0;
        }
    }

    private static class MyGroupingComparator extends WritableComparator {

        public MyGroupingComparator() {
            super(Bean.class, true);
        }


        @Override
        public int compare(WritableComparable a, WritableComparable b) {
            return 0;
        }
    }

    public static void main(String[] args) throws Exception {
        Job job = Job.getInstance();
        job.setJobName("WordCount");

        job.setJarByClass(MRWordCount.class);

        // job.addCacheFile(new URI("/position"));

        job.setMapperClass(MyMapper.class);
        job.setMapOutputKeyClass(Bean.class);
        job.setMapOutputValueClass(NullWritable.class);

        job.setReducerClass(MyReducer.class);
        job.setOutputKeyClass(Bean.class);
        job.setOutputValueClass(NullWritable.class);
        job.setNumReduceTasks(2);

        job.setPartitionerClass(MyPartitioner.class);

        job.setSortComparatorClass(MySortComparator.class);
        job.setGroupingComparatorClass(MyGroupingComparator.class);

        FileInputFormat.addInputPath(job, new Path("/word"));
        FileOutputFormat.setOutputPath(job, new Path("/output"));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
