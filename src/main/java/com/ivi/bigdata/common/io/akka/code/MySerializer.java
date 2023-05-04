package com.ivi.bigdata.common.io.akka.code;

import akka.serialization.JSerializer;

public class MySerializer extends JSerializer {
    @Override
    public Object fromBinaryJava(byte[] bytes, Class<?> manifest) {
        return null;
    }

    @Override
    public int identifier() {
        return 0;
    }

    @Override
    public byte[] toBinary(Object o) {
        return new byte[0];
    }

    @Override
    public boolean includeManifest() {
        return false;
    }
}
