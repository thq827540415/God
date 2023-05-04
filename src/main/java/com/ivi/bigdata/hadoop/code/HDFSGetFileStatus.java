package com.ivi.bigdata.hadoop.code;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.hdfs.DistributedFileSystem;

import java.net.URI;
import java.util.Arrays;

/**
 * 对HDFS的重要操作
 */
public class HDFSGetFileStatus {
    public static void main(String[] args) {
        try (FileSystem fs = DistributedFileSystem.newInstance(
                new URI("hdfs://bigdata01:9000"),
                new Configuration(),
                "root")) {

            // 通过DistributedFileSystem中特有的listStatus
            // 如果路径是directory，则会列出路径下所有的文件信息，如果是file，则列出该文件的信息
            // path、isDirectory、length => size、replication、blocksize、modification_time、access_time、owner、group、permission、isSymlink
            for (FileStatus fileStatus : fs.listStatus(new Path("/input"))) {
                System.out.println(fileStatus.getPath());
                System.out.println(fileStatus);
            }

            System.out.println("==========================================");

            // 通过FileSystem中的globStatus，由正则表达式获取
            // 只获取匹配上的路径的文件信息，包括文件和文件夹，但不会获取路径下的内容
            for (FileStatus fileStatus : fs.globStatus(new Path("/input"))) {
                System.out.println(fileStatus.getPath());
                System.out.println(fileStatus);
            }

            System.out.println("==========================================");

            RemoteIterator<LocatedFileStatus> iter = fs.listLocatedStatus(new Path("/input"));
            while (iter.hasNext()) {
                LocatedFileStatus locatedFileStatus = iter.next();
                System.out.println(locatedFileStatus.getPath());
                // 相比于FileStatus，LocatedFileStatus能够获取数据块信息 => LocatedFileStatus extends FileStatus
                if (locatedFileStatus.isFile()) {
                    System.out.println(Arrays.toString(locatedFileStatus.getBlockLocations()));
                }
                System.out.println(locatedFileStatus);
            }

            System.out.println("==========================================");

            // 如果path是文件夹，则会列出该文件夹下的文件的信息，recursive为true则递归访问path下的所有的文件信息；
            // 如果path是文件，则直接访问该文件的信息
            RemoteIterator<LocatedFileStatus> listFiles = fs.listFiles(new Path("/input/word.txt"), true);
            while (listFiles.hasNext()) {
                LocatedFileStatus status = listFiles.next();
                System.out.println(status.getPath());
                System.out.println(status);
            }
        } catch (Exception ignored) {
        }
    }
}
