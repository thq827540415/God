package com.lumine.bigdata.hbase;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;

import java.io.IOException;

@Slf4j
public class E01_BasicApi {
    private static final Connection CONN;

    static {
        try {
            Configuration conf = HBaseConfiguration.create();
            conf.set("hbase.zookeeper.quorum", "bigdata01,bigdata02,bigdata03");
            CONN = ConnectionFactory.createConnection(conf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private static void createNs() {
        try (Admin admin = CONN.getAdmin()) {
            // 自动负载均衡
            // admin.balancer(true);
            NamespaceDescriptor descriptor = NamespaceDescriptor.create("ns")
                    .addConfiguration("author", "lumine")
                    .build();

            admin.createNamespace(descriptor);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

            TableName student = TableName.valueOf("student");

            if (!admin.tableExists(student)) {

            } else {
                log.info("{} not exists", student.getNameAsString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        // createNs();
        // getNs();
        delNs();

    }
}
