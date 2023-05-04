package com.ivi.design.creation.factory.method;

import com.ivi.design.creation.factory.parser.rule.RuleConfigParser;

// 工厂方法比简单工厂更加符合开闭原则
public interface RuleConfigParserFactory {
    RuleConfigParser createParser();
}
