package com.ivi.design.creation.factory.abs;

import com.ivi.design.creation.factory.parser.rule.RuleConfigParser;
import com.ivi.design.creation.factory.parser.system.SystemConfigParser;

// 抽象工厂
public interface AbstractConfigParserFactory {
    RuleConfigParser createRuleParser();
    SystemConfigParser createSystemParser();
}
