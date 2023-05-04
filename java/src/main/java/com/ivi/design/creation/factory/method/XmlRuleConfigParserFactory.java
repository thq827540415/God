package com.ivi.design.creation.factory.method;

import com.ivi.design.creation.factory.parser.rule.RuleConfigParser;
import com.ivi.design.creation.factory.parser.rule.XmlRuleConfigParser;

public class XmlRuleConfigParserFactory implements RuleConfigParserFactory {
    @Override
    public RuleConfigParser createParser() {
        return new XmlRuleConfigParser();
    }
}
