package com.ivi.design.creation.factory.simple;

import com.ivi.design.creation.factory.parser.rule.*;

import java.util.HashMap;
import java.util.Map;

// 简单工厂模式类
public class RuleConfigParserFactory {

    private static final Map<String, RuleConfigParser> cachedParsers = new HashMap<>();

    static {
        // 如果JsonRuleConfigParser()的初始化逻辑很复杂，则需要另一个类对这段逻辑进行封装
        // 此时采用的就是工厂方法
        cachedParsers.put("json", new JsonRuleConfigParser());
        cachedParsers.put("xml", new XmlRuleConfigParser());
        cachedParsers.put("yaml", new YamlRuleConfigParser());
        cachedParsers.put("properties", new PropertiesRuleConfigParser());
    }

    // 简单工厂的第二种实现方式，类似单例中的多例模式 + 工厂
    public static RuleConfigParser createParser(String configFormat) {

        if (configFormat == null || configFormat.isEmpty()) {
            return null;
        }
        return cachedParsers.get(configFormat.toLowerCase());
    }

    // 简单工厂的第一种实现方式
    public static RuleConfigParser createParser1(String configFormat) {
        RuleConfigParser parser = null;
        if ("json".equalsIgnoreCase(configFormat)) {
            parser = new JsonRuleConfigParser();
        } else if ("xml".equalsIgnoreCase(configFormat)) {
            parser = new XmlRuleConfigParser();
        } else if ("yaml".equalsIgnoreCase(configFormat)) {
            parser = new YamlRuleConfigParser();
        } else if ("properties".equalsIgnoreCase(configFormat)) {
            parser = new PropertiesRuleConfigParser();
        }
        return parser;
    }
}
