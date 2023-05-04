package com.ivi.design.creation.factory;

import com.ivi.design.creation.factory.abs.ConfigParserFactoryMap;
import com.ivi.design.creation.factory.method.RuleConfigParserFactoryMap;
import com.ivi.design.creation.factory.parser.rule.RuleConfigParser;
import com.ivi.design.creation.factory.simple.RuleConfigParserFactory;

import java.util.Optional;

public class RuleConfigSource {
    public static class RuleConfig {
    }

    public RuleConfig load(String ruleConfigFilePath) {
        String ruleConfigExtension = getFileExtension(ruleConfigFilePath);

        // 为了让代码逻辑更加清晰，可读性更好，需要将功能独立的代码块封装成函数
        // 同时为了让类的职责更加单一，代码更加清晰，将createParser剥离到一个独立类中
        // 而这个独立的类就是简单工厂
        RuleConfigParser parser =
                RuleConfigParserFactory.createParser(ruleConfigExtension);

        // 工厂方法
        RuleConfigParser parser1 =
                RuleConfigParserFactoryMap.getParserFactory(ruleConfigExtension)
                        .createParser();

        // 抽象工厂 + 工厂方法
        RuleConfigParser parser2 =
                ConfigParserFactoryMap.getParserFactory(ruleConfigExtension)
                        .createRuleParser();

        String configText = "";

        return Optional.of(parser)
                .map(p -> p.parse(configText))
                .orElse(null);
    }

    private String getFileExtension(String filePath) {
        // 。。。
        return "json";
    }
}
