package com.ivi.design.creation.factory.method;

import java.util.HashMap;
import java.util.Map;

// 这里是简单工厂
public class RuleConfigParserFactoryMap {
    private static final Map<String, RuleConfigParserFactory> cachedFactories =
            new HashMap<>();

    static {
        // 如果初始化逻辑很复杂，则采用Factory对其进行封装
        cachedFactories.put("json", new JsonRuleConfigParserFactory());
        cachedFactories.put("xml", new XmlRuleConfigParserFactory());
        cachedFactories.put("yaml", new YamlRuleConfigParserFactory());
        cachedFactories.put("properties", new PropertiesRuleConfigParserFactory());
    }

    public static RuleConfigParserFactory getParserFactory(String type) {
        if (type == null || type.isEmpty()) {
            return null;
        }
        return cachedFactories.get(type.toLowerCase());
    }
}
