package com.solitude.basic.serializer.protobuf;

import lombok.Cleanup;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 为我们的提供了更强的类，可以在此上进行序列化和反序列化
 */
public class ProtoBuf {

    private static final String OUTPUT_PATH = "./output/addressbook.data";


    /*
    public static void main(String[] args) throws IOException {
        // todo 1. Designing objects
        // todo 2. Describing objects

        // todo 3. Compiling the description
        // （1）通过命令行编译${PROTOC_HOME}/protoc --java_out=$DST_DIR addressbook.proto
        // （2）通过maven插件编译 --> sb

        // todo 4. Obtaining the generated source-code
        // todo 5. Importing objects into ur project

        // todo 6. Instantiating objects
        AddressBookProtos.Person john = AddressBookProtos.Person.newBuilder()
                .setId(123)
                .setName("john")
                .setEmail("827540415@qq.com")
                .addPhone(
                        AddressBookProtos.Person.PhoneNumber.newBuilder()
                                .setNumber("+351 999 999 999")
                                .setType(AddressBookProtos.Person.PhoneType.HOME)
                                .build())
                .addPhone(
                        AddressBookProtos.Person.PhoneNumber.newBuilder()
                                .setNumber("15576099565")
                                .setType(AddressBookProtos.Person.PhoneType.MOBILE)
                                .build())
                .build();

        // todo 7. using objects
        // （1）writing data to a file
        @Cleanup FileOutputStream fos = new FileOutputStream(OUTPUT_PATH);
        john.writeTo(fos);

        // （2）reading data from a file
        @Cleanup FileInputStream fis = new FileInputStream(OUTPUT_PATH);
        AddressBookProtos.Person person = AddressBookProtos.Person.parseFrom(fis);
        System.out.println(person);
    }*/
}
