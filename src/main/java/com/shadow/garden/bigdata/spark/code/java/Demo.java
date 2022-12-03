package com.shadow.garden.bigdata.spark.code.java;

import java.io.File;
import java.sql.SQLException;

public class Demo {

    public static class Father {
        public void say() {
            System.out.println("father");
        }

        public void sing() {
            System.out.println("father sing");
        }
    }

    static class Peo extends Father {
        @Override
        public void say() {
            System.out.println("peo");
        }

        public void sing() {
            System.out.println("peo sing");
        }
    }

    static class Son extends Peo {
        @Override
        public void say() {
            System.out.println("son");
        }

        @Override
        public void sing() {
            System.out.println("son sing");
        }
    }

    public static void main(String[] args) throws SQLException {
        Father f = new Son();
        ((Peo) f).sing();


        File file = new File("C:\\Users\\82754\\Desktop\\21-Flink城市交通实时监控平台");
        File[] files = file.listFiles();
        for (File file1 : files) {
            if (file1.isFile()) {
                String fileName = file1.getName().replaceAll("【.+】", "");
                file1.renameTo(new File(file1.getParentFile() + "\\" + fileName));
            }
        }
    }
}
