package com.solitude.basic.serializer.avro;

import lombok.Cleanup;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.File;
import java.io.IOException;

/**
 * Avro schemas are defined with JSON
 * The Schema is stored along with the Avro data in a file
 */
public class Avro {

    private static final String OUTPUT_PATH = "./output/emp.avro";


    public static void main(String[] args) throws IOException {
        // todo 1. Create Schemas
        // todo 2. Read the schemas into ur program
        // （1）By Generating a class corresponding to Schema
        // java -jar avro-tools-x.x.x.jar compile schema xxx.avsc ./ --> 得到xxx.java文件

        // （2）By using parsers libray
        // 通过API读取xxx.avsc文件
        Schema schema = getSchemaByClasspathFile();

        // todo 3. Serializer the data using the serialization API provided for Avro
        writeBySchemeFile(schema);

        // todo 4. Deserializer the data using the deserialization API provided for Avro
        // （1）通过代码获取.avro文件数据
        readBySchema(schema);

        // （2）通过命令获取.avro文件数据
        // java -jar avro-tools-x.x.x.jar tojson xxx.avro
    }


    // ===================================== serializer ====================================
    private static void writeByCompileFile() throws IOException {
        SpecificDatumWriter<Emp> specificDatumWriter = new SpecificDatumWriter<>(Emp.class);
        Emp emp1 = new Emp("zs", 1, 100, 21, "china");
        Emp emp2 = new Emp("ls", 2, 200, 18, "china");
        @Cleanup DataFileWriter<Emp> empDataFileWriter = new DataFileWriter<>(specificDatumWriter);
        empDataFileWriter.create(Emp.SCHEMA$, new File(OUTPUT_PATH));

        empDataFileWriter.append(emp1);
        empDataFileWriter.append(emp2);
    }

    private static void writeBySchemeFile(Schema schema) throws IOException {
        GenericData.Record record = new GenericData.Record(schema);
        record.put("name", "zs");
        record.put("id", 1);
        record.put("salary", 100);
        record.put("age", 19);
        record.put("address", "china");

        GenericDatumWriter<GenericRecord> genericDatumWriter = new GenericDatumWriter<>(schema);
        @Cleanup DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<>(genericDatumWriter);
        dataFileWriter.create(schema, new File(OUTPUT_PATH));

        dataFileWriter.append(record);
    }

    // ==================================== deserializer ===================================
    private static void readByCompileFile() throws IOException {
        SpecificDatumReader<Emp> specificDatumReader = new SpecificDatumReader<>(Emp.class);
        @Cleanup DataFileReader<Emp> emps = new DataFileReader<>(new File(OUTPUT_PATH), specificDatumReader);

        Emp reuse = null;
        while (emps.hasNext()) {
            reuse = emps.next(reuse);
            System.out.println(reuse);
        }
    }

    private static void readBySchema(Schema schema) throws IOException {
        GenericDatumReader<GenericRecord> genericDatumReader = new GenericDatumReader<>(schema);
        @Cleanup DataFileReader<GenericRecord> records =
                new DataFileReader<>(new File(OUTPUT_PATH), genericDatumReader);

        GenericRecord record = null;
        while (records.hasNext()) {
            record = records.next(record);
            System.out.println(record);
        }
    }


    // ======================================= schema ======================================
    private static Schema getSchemaByFile() throws IOException {
        return new Schema.Parser()
                .parse(new File("D:\\IdeaProjects\\Algorithm\\src\\main\\resources\\avro\\emp.avsc"));
    }

    private static Schema getSchemaByClasspathFile() throws IOException {
        return new Schema.Parser()
                .parse(Avro.class.getClassLoader().getResourceAsStream("avro/emp.avsc"));
    }
}
