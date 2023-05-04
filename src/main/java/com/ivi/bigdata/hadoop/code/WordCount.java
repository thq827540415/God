package com.ivi.bigdata.hadoop.code;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
public class WordCount {
    static {
        System.setProperty("HADOOP_USER_NAME", "root");
    }

    private static class WCMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
        Text k = new Text();
        static final IntWritable v = new IntWritable(1);

        @Override
        protected void map(LongWritable key, Text value,
                           Context context) throws IOException, InterruptedException {

            Thread.sleep(Integer.MAX_VALUE);
            Arrays.stream(value.toString().split("\\s+"))
                    .forEach(word -> {
                        try {
                            k.set(word);
                            context.write(k, v);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    private static class WCReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        final IntWritable v = new IntWritable();

        @Override
        protected void reduce(Text k, Iterable<IntWritable> values,
                              Context context) throws IOException, InterruptedException {
            int sum = 0;
            for (IntWritable value : values) {
                sum += value.get();
            }
            v.set(sum);
            context.write(k, v);
        }
    }

    /**
     * 使用hadoop jar提交，需要指定com.solitude.bigdata.hadoop.code.MRWordCount.WCDriver，使用反射获取main方法
     */
    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
        Job job = Job.getInstance(new Configuration(), "inner wc");

        // uber mode指MapTask和ReduceTask不在新的container进程里面跑，而是在AM所在节点的JVM以线程的形式运行。
        // 在MRAppMaster初始化的时候会判断当前作业是否适用于uber模式
        // job.isUber();

        job.setJarByClass(WordCount.class);

        job.setMapperClass(WCMapper.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);

        job.setReducerClass(WCReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(job, new Path("/input/word.snappy"));
        FileOutputFormat.setOutputPath(job, new Path("/output"));

        boolean flag = job.waitForCompletion(true);
        System.exit(flag ? 0 : 1);
    }
}
