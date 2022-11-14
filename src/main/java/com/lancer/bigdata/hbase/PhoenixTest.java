package com.lancer.bigdata.hbase;

import org.apache.phoenix.jdbc.PhoenixDriver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class PhoenixTest {
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        Properties props = new Properties();
        props.setProperty("phoenix.schema.isNamespaceMappingEnabled", "true");

        Class.forName(PhoenixDriver.class.getName());
        Connection conn = DriverManager.getConnection("jdbc:phoenix:192.168.31.237:2182", props);
        System.out.println(conn);
    }
}