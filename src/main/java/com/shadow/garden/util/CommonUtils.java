package com.shadow.garden.util;


import java.util.concurrent.TimeUnit;

public class CommonUtils {

    /**
     * 由于字符串比较可以通过时间破解密码 -> 计时攻击
     */
    public static boolean safeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int equal = 0;
        // Compares two strings using the same time
        for (int i = 0; i < a.length(); i++) {
            equal |= a.charAt(i) ^ b.charAt(i);
        }
        return equal == 0;
    }

    /**
     * 封装睡眠时间
     */
    public static void sleep(long timeout, TimeUnit unit) {
        try {
            unit.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
