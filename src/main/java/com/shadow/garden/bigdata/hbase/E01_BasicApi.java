package com.shadow.garden.bigdata.hbase;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;

import java.io.IOException;
import java.util.Map;

@Slf4j
public class E01_BasicApi {
    private static final Connection CONN;

    static {
        try {
            Configuration conf = HBaseConfiguration.create();
            conf.set("hbase.zookeeper.quorum", "bigdata01,bigdata02,bigdata03");
            conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
            CONN = ConnectionFactory.createConnection(conf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        createNs();
        // getNs();
        // delNs();

    }

    private static void createNs() {
        try (Admin admin = CONN.getAdmin()) {
            // 自动负载均衡
            // admin.balancer(true);
            NamespaceDescriptor descriptor = NamespaceDescriptor.create("ns")
                    .addConfiguration("author", "shadow")
                    .build();

            admin.createNamespace(descriptor);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * list_namespace
     */
    private static void getNs() {
        try (Admin admin = CONN.getAdmin()) {
            // todo 1. 获取单个Namespace
            // NamespaceDescriptor descriptor = admin.getNamespaceDescriptor("ns");

            // todo 2. 获取所有Namespace
            for (NamespaceDescriptor descriptor : admin.listNamespaceDescriptors()) {
                String nsName = descriptor.getName();
                if ("ns".equals(nsName)) {
                    log.info("ns ==> author: {}", descriptor.getConfigurationValue("author"));
                } else {
                    log.info("{}", nsName);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * drop_namespace
     */
    private static void delNs() {
        try (Admin admin = CONN.getAdmin()) {
            // ns内容为空
            admin.deleteNamespace("ns");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * create 'ns:student', 'info'
     */
    private static void createTable() {
        try (Admin admin = CONN.getAdmin()) {

            TableName student = TableName.valueOf("ns:student");

            if (!admin.tableExists(student)) {
                // 新版API --> TableDescriptorBuilder + ColumnFamilyDescriptorBuilder
                HTableDescriptor tableDesc = new HTableDescriptor(student);
                HColumnDescriptor columnDesc = new HColumnDescriptor("info");
                tableDesc.addFamily(columnDesc);
                admin.createTable(tableDesc);
            } else {
                log.error("{} not exists", student.getNameAsString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 支持正则
     * list 'ns:.*'
     */
    private static void listTable() {
        try (Admin admin = CONN.getAdmin()) {

            // 获取所有表
            admin.listTables();

            // 获取namespace下的所有表
            HTableDescriptor[] tableDescriptors = admin.listTableDescriptorsByNamespace("ns");

            for (HTableDescriptor tableDesc : tableDescriptors) {
                log.info("ns --> {}, table --> {}",
                        tableDesc.getTableName().getNamespaceAsString(),
                        tableDesc.getNameAsString());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * describe 'ns:student'
     */
    private static void descTable() {
        try (Admin admin = CONN.getAdmin()) {

            TableName student = TableName.valueOf("ns:student");

            HTableDescriptor tableDesc = admin.getTableDescriptor(student);

            // 获取表的所有列簇
            // HColumnDescriptor[] columnFamilies = tableDesc.getColumnFamilies();

            tableDesc.getFamilies()
                    .forEach(
                            columnDesc -> {
                                // 获取每个列簇的名字
                                log.info("colum family name --> {}, maxVersion --> {}, ttl --> {}",
                                        columnDesc.getNameAsString(),
                                        columnDesc.getMaxVersions(),
                                        columnDesc.getTimeToLive());

                                // 获取单个列簇的所有配置项
                                Map<String, String> perColumnConfiguration = columnDesc.getConfiguration();

                                // 获取元数据信息
                                String ttl1 = columnDesc.getValue("TTL");

                                // 根据某个具体配置项
                                String ttl2 = columnDesc.getConfigurationValue("TTL");
                            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
