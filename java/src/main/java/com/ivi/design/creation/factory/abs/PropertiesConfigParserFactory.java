package com.ivi.design.creation.factory.abs;

import com.ivi.design.creation.factory.parser.rule.PropertiesRuleConfigParser;
import com.ivi.design.creation.factory.parser.rule.RuleConfigParser;
import com.ivi.design.creation.factory.parser.system.PropertiesSystemConfigParser;
import com.ivi.design.creation.factory.parser.system.SystemConfigParser;

public class PropertiesConfigParserFactory implements AbstractConfigParserFactory {
    @Override
    public RuleConfigParser createRuleParser() {
        return new PropertiesRuleConfigParser();
    }

    @Override
    public SystemConfigParser createSystemParser() {
        return new PropertiesSystemConfigParser();
    }
}
