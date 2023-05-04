package com.ivi.design.creation.factory.method;

import com.ivi.design.creation.factory.parser.rule.RuleConfigParser;
import com.ivi.design.creation.factory.parser.rule.JsonRuleConfigParser;

public class JsonRuleConfigParserFactory implements RuleConfigParserFactory {
    @Override
    public RuleConfigParser createParser() {
        return new JsonRuleConfigParser();
    }
}
