package com.ivi.bigdata.hadoop.code;

import org.apache.hadoop.hive.ql.exec.UDFArgumentException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentLengthException;
import org.apache.hadoop.hive.ql.exec.UDFArgumentTypeException;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.parse.SemanticException;
import org.apache.hadoop.hive.ql.udf.generic.*;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.PrimitiveObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;

import java.util.ArrayList;
import java.util.List;

public class UserDefinedFunction {

    static class MyUDF extends GenericUDF {
        @Override
        public ObjectInspector initialize(ObjectInspector[] objectInspectors) throws UDFArgumentException {

            if (objectInspectors.length != 1) {
                throw new UDFArgumentLengthException("Length Error");
            }

            if (!objectInspectors[0].getCategory().equals(ObjectInspector.Category.PRIMITIVE)) {
                throw new UDFArgumentTypeException(0, "Type Error");
            }
            return PrimitiveObjectInspectorFactory.javaIntObjectInspector;
        }

        @Override
        public Object evaluate(DeferredObject[] deferredObjects) throws HiveException {
            Object str = deferredObjects[0].get();
            if (str == null) {
                return 0;
            }
            return str.toString().length();
        }

        @Override
        public String getDisplayString(String[] strings) {
            return "";
        }
    }



    static class MyUDTF extends GenericUDTF {
        @Override
        public StructObjectInspector initialize(StructObjectInspector argOIs) throws UDFArgumentException {
            List<String> structFieldNames = new ArrayList<>();
            List<ObjectInspector> structFieldObjectInspectors = new ArrayList<>();

            structFieldNames.add("lineToWord");
            structFieldObjectInspectors.add(PrimitiveObjectInspectorFactory.javaStringObjectInspector);

            return ObjectInspectorFactory.getStandardStructObjectInspector(structFieldNames, structFieldObjectInspectors);
        }

        @Override
        public void process(Object[] objects) throws HiveException {
            String arg = objects[0].toString();
            String splitKey = objects[1].toString();
            String[] fields = arg.split(splitKey);

            for (String field : fields) {
                forward(field);
            }
        }

        @Override
        public void close() throws HiveException {
        }
    }



    static class MyUDAF extends GenericUDAFSum {
        @Override
        public GenericUDAFEvaluator getEvaluator(GenericUDAFParameterInfo genericUDAFParameterInfo) throws SemanticException {
            return null;
        }

        @Override
        public GenericUDAFEvaluator getEvaluator(TypeInfo[] typeInfos) throws SemanticException {
            return null;
        }
    }
}

