package com.ava.bigdata.hadoop.code;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.xerial.snappy.SnappyOutputStream;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Arrays;

public class HDFSGetFileStatus {
    public static void main(String[] args) throws Exception {
        FileSystem fs = DistributedFileSystem.newInstance(
                new URI("hdfs://bigdata01:9000"),
                new Configuration(),
                "root");


        // 通过DistributedFileSystem中特有的的listStatus
        // 如果路径是directory，则会列出路径下所有的文件信息，如果是file，则列出该文件的信息
        for (FileStatus fileStatus : fs.listStatus(new Path("/input"))) {
            System.out.println(fileStatus.getPath());
            System.out.println(fileStatus);
        }

        System.out.println("==========================================");

        // 通过FileSystem中的globStatus，由正则表达式获取
        // 只获取匹配上的路径的文件信息，不会获取路径下的内容
        for (FileStatus fileStatus : fs.globStatus(new Path("/input"))) {
            System.out.println(fileStatus.getPath());
            System.out.println(fileStatus);
        }

        System.out.println("==========================================");

        // 相比于FileStatus，LocatedFileStatus能够获取数据块信息
        RemoteIterator<LocatedFileStatus> iter = fs.listLocatedStatus(new Path("/input"));
        while (iter.hasNext()) {
            LocatedFileStatus locatedFileStatus = iter.next();
            System.out.println(locatedFileStatus.getPath());
            // System.out.println(Arrays.toString(locatedFileStatus.getBlockLocations()));
            System.out.println(locatedFileStatus);
        }

        System.out.println("==========================================");

        RemoteIterator<LocatedFileStatus> iterator = fs.listLocatedStatus(new Path("/input/word.snappy"));
        while (iterator.hasNext()) {
            LocatedFileStatus next = iterator.next();

            BlockLocation[] blockLocations = next.getBlockLocations();

            System.out.println(blockLocations[0].getLength());
        }


        fs.close();





        // compress();
    }

    private static void compress() throws IOException, InterruptedException {
        File distPath = new File("D:\\word.snappy");

        byte[] buf = new byte[8 * 1024 * 1024];
        Arrays.fill(buf, (byte) 1);
        int count = 0;
        SnappyOutputStream out = new SnappyOutputStream(Files.newOutputStream(distPath.toPath()));
        while (true) {
            out.write(buf, 0, buf.length);
            count += 8;
            if (count == 5 * 1024) {
                break;
            }
            Thread.sleep(100);
        }
        out.close();
    }
}
