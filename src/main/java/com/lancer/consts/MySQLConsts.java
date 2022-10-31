package com.lancer.consts;

/**
 * @Author lancer
 * @Date 2022/6/15 21:53
 * @Description
 */
public class MySQLConsts {

    public static final String MYSQL_HOST = "bigdata03";

    public static final int MYSQL_PORT = 3306;

    public static final String MYSQL_URL = "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/test?useSSL=false&useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true";
    public static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver";
    public static final String MYSQL_USERNAME = "root";
    public static final String MYSQL_PASSWORD = "123456";
}
