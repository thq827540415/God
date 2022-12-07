package com.shadow.garden.bigdata.hbase.code;

import com.solitude.util.BasicEnvUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;

import java.io.IOException;

@Slf4j
public class E01_BasicApi {
    private static final Connection conn = BasicEnvUtils.getHBaseInstance();

    public static void main(String[] args) throws IOException {
        // createNs();
        // getNs();
        // alterNs();
        // delNs();

        // dropTable();
        conn.close();
    }

    // todo ================================= namespace ==================================

    /**
     * create_namespace 'ns', {author => 'shadow'}
     */
    private static void createNs() {
        try (Admin admin = conn.getAdmin()) {
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
     * alter_namespace 'ns', {author => 'garden'}
     */
    private static void alterNs() {
        try (Admin admin = conn.getAdmin()) {
            // 获取单个Namespace
            NamespaceDescriptor ns = admin.getNamespaceDescriptor("ns");
            // 删除某个配置
            ns.removeConfiguration("author");
            // 覆盖之前的配置
            ns.setConfiguration("author", "garden");
            admin.modifyNamespace(ns);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 支持正则
     * list_namespace '*'
     * 不支持正则
     * describe_namespace 'ns'
     */
    private static void getNs() {
        try (Admin admin = conn.getAdmin()) {
            // 获取所有Namespace
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
        try (Admin admin = conn.getAdmin()) {
            // ns内容为空
            admin.deleteNamespace("ns");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // todo ================================= table ==================================

    /**
     * create 'ns:student', 'info'
     */
    private static void createTable() {
        try (Admin admin = conn.getAdmin()) {

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
        try (Admin admin = conn.getAdmin()) {
            // 获取所有TableDescriptor
            // admin.listTables();

            // 获取所有表名
            // admin.listTableNames();

            // 获取namespace下的所有TableDescriptor
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
        try (Admin admin = conn.getAdmin()) {

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
                                log.info("getConfiguration() --> {}", columnDesc.getConfiguration());

                                // 获取元数据信息
                                log.info("getValue() --> {}", columnDesc.getValue("TTL"));

                                // 获取某个具体配置项
                                log.info("getConfigurationValue() --> {}",
                                        columnDesc.getConfigurationValue("TTL"));
                            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * disable 'ns:student' -> drop 'ns:student'
     */
    private static void dropTable() {
        try (Admin admin = conn.getAdmin()) {
            TableName student = TableName.valueOf("ns:student");
            if (admin.tableExists(student)) {
                if (!admin.isTableDisabled(student)) {
                    admin.disableTable(student);
                }
                admin.deleteTable(student);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * put 'ns:t', 'rk', 'c', 'value' --> 效率低
     * deleteall 'ns:t', 'rk', 'column'
     * get 'ns:t', 'rk', 'c1', 'c2'
     */
    private static void crudTable() {
        try (Table table = conn.getTable(TableName.valueOf("ns:student"))) {
            // 一个rowKey对应一个Put对象、Delete对象、Get对象
            // addColumn添加列，addFamily添加列簇
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * scan 'ns:t', {COLUMNS => ['info', 'c2'], LIMIT => 10, STARTROW => 'rk001'}
     */
    private static void scanTable() {
        try (Table table = conn.getTable(TableName.valueOf("ns:student"))) {
            Scan scan = new Scan();

            scan.withStartRow("rk001".getBytes());
            scan.withStopRow("rk002".getBytes(), true);
            // 设置显示条数
            scan.setLimit(10);
            // 设置列簇
            scan.addFamily("info".getBytes());

            table.getScanner(scan)
                    .forEach(
                            result -> {
                                while (result.advance()) {
                                    Cell cell = result.current();
                                    log.info("rowKey --> {}, family --> {}, qualifier --> {}, value --> {}, ts --> {}",
                                            CellUtil.cloneRow(cell),
                                            CellUtil.cloneFamily(cell),
                                            CellUtil.cloneQualifier(cell),
                                            CellUtil.cloneValue(cell),
                                            cell.getTimestamp());
                                }
                            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}