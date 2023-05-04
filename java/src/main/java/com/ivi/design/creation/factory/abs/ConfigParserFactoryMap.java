package com.ivi.design.creation.factory.abs;

import java.util.HashMap;
import java.util.Map;

public class ConfigParserFactoryMap {
    private static final Map<String, AbstractConfigParserFactory> cachedFactoryMap  =
            new HashMap<>();

    static {
        cachedFactoryMap.put("json", new JsonConfigParserFactory());
        cachedFactoryMap.put("xml", new XmlConfigParserFactory());
        cachedFactoryMap.put("yaml", new YamlConfigParserFactory());
        cachedFactoryMap.put("properties", new PropertiesConfigParserFactory());
    }

    public static AbstractConfigParserFactory getParserFactory(String type) {
        if (type == null || type.isEmpty()) {
            return null;
        }
        return cachedFactoryMap.get(type.toLowerCase());
    }
}
