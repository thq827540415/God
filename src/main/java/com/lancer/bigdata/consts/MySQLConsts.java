package com.lancer.bigdata.consts;

/**
 * @Author lancer
 * @Date 2022/6/15 21:53
 * @Description
 */
public class MySQLConsts {

    public static final String HOST = "bigdata03";

    public static final int PORT = 3306;

    /**
     * 需要修改数据库
     */
    public static final String URL =
            "jdbc:mysql://" + HOST + ":" + PORT +
                    "/test?useSSL=false&useUnicode=true&characterEncoding=utf8&allowPublicKeyRetrieval=true";
    public static final String DRIVER = "com.mysql.jdbc.Driver";
    public static final String USERNAME = "root";
    public static final String PASSWORD = "123456";
}
