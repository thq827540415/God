package com.ava.bigdata.common.serializer.parquet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.column.ParquetProperties;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.SimpleGroupFactory;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.example.GroupReadSupport;
import org.apache.parquet.hadoop.example.GroupWriteSupport;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;

import java.io.IOException;

public class Parquet {
    private static void write2Parquet() {
        MessageType schema = MessageTypeParser.parseMessageType(
                "message Person {\n" +
                        "   required binary name;\n" +
                        "   required int32 age;\n" +
                        "   required int32 favorite_number;\n" +
                        "   required binary favorite_color;\n" +
                        "}");

        GroupWriteSupport groupWriteSupport = new GroupWriteSupport();
        GroupWriteSupport.setSchema(schema, new Configuration());
        try (ParquetWriter<Group> writer = new ParquetWriter<>(
                new Path("./person.parquet"),
                groupWriteSupport,
                CompressionCodecName.SNAPPY,
                ParquetWriter.DEFAULT_BLOCK_SIZE,
                ParquetWriter.DEFAULT_PAGE_SIZE,
                ParquetWriter.DEFAULT_PAGE_SIZE,
                ParquetWriter.DEFAULT_IS_DICTIONARY_ENABLED,
                ParquetWriter.DEFAULT_IS_VALIDATING_ENABLED,
                ParquetProperties.WriterVersion.PARQUET_1_0, new Configuration())) {

            Group group = new SimpleGroupFactory(schema)
                    .newGroup()
                    .append("name", "zs")
                    .append("age", 20)
                    .append("favorite_number", 3)
                    .append("favorite_color", "red");

            writer.write(group);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * hadoop jar parquet-tools.jar meta|schema|dump
     */
    private static void readParquet() {
        GroupReadSupport groupReadSupport = new GroupReadSupport();
        try (ParquetReader<Group> reader =
                     new ParquetReader<>(new Path(""), groupReadSupport)) {

            Group result = reader.read();
            System.out.println(result.getString("name", 0));
            System.out.println(result.getString("age", 0));
            System.out.println(result.getString("favorite_number", 0));
            System.out.println(result.getString("favorite_color", 0));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {


    }
}
