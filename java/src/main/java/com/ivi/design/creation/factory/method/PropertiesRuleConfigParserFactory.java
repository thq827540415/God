package com.ivi.design.creation.factory.method;

import com.ivi.design.creation.factory.parser.rule.PropertiesRuleConfigParser;
import com.ivi.design.creation.factory.parser.rule.RuleConfigParser;

public class PropertiesRuleConfigParserFactory implements RuleConfigParserFactory {
    @Override
    public RuleConfigParser createParser() {
        return new PropertiesRuleConfigParser();
    }
}
