package com.ivi.design.creation.factory.abs;

import com.ivi.design.creation.factory.parser.rule.JsonRuleConfigParser;
import com.ivi.design.creation.factory.parser.rule.RuleConfigParser;
import com.ivi.design.creation.factory.parser.rule.XmlRuleConfigParser;
import com.ivi.design.creation.factory.parser.system.JsonSystemConfigParser;
import com.ivi.design.creation.factory.parser.system.SystemConfigParser;
import com.ivi.design.creation.factory.parser.system.XmlSystemConfigParser;

public class XmlConfigParserFactory implements AbstractConfigParserFactory {
    @Override
    public RuleConfigParser createRuleParser() {
        return new XmlRuleConfigParser();
    }

    @Override
    public SystemConfigParser createSystemParser() {
        return new XmlSystemConfigParser();
    }
}
