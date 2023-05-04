package com.ivi.consts;

public class MysqlConstants {
    public static final String HOST = "bigdata03";

    public static final int PORT = 3306;

    private static final String DATABASE = "test";

    /**
     * 需要修改数据库
     */
    public static final String URL =
            "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE
                    + "?useSSL=false&useUnicode=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=true";
    public static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    public static final String USERNAME = "root";
    public static final String PASSWORD = "123456";
}
