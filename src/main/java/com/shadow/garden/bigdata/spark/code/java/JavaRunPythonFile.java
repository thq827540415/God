package com.shadow.garden.bigdata.spark.code.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class JavaRunPythonFile {
    public static void main(String[] args) throws IOException, InterruptedException {
        String cmd = "python ./python/test.py";
        Process proc = Runtime.getRuntime().exec(cmd);
        InputStream is = proc.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String str = br.readLine();
        proc.waitFor();
        System.out.println(str);
        br.close();
        is.close();
    }
}
