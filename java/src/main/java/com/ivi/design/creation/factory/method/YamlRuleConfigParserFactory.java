package com.ivi.design.creation.factory.method;

import com.ivi.design.creation.factory.parser.rule.RuleConfigParser;
import com.ivi.design.creation.factory.parser.rule.YamlRuleConfigParser;

public class YamlRuleConfigParserFactory implements RuleConfigParserFactory {
    @Override
    public RuleConfigParser createParser() {
        return new YamlRuleConfigParser();
    }
}
