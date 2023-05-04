package com.ivi.juc.code;

import java.util.concurrent.TimeUnit;

public class CommonUtils {
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
