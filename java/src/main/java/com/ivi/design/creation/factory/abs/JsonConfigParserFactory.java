package com.ivi.design.creation.factory.abs;

import com.ivi.design.creation.factory.parser.rule.JsonRuleConfigParser;
import com.ivi.design.creation.factory.parser.rule.RuleConfigParser;
import com.ivi.design.creation.factory.parser.system.JsonSystemConfigParser;
import com.ivi.design.creation.factory.parser.system.SystemConfigParser;

public class JsonConfigParserFactory implements AbstractConfigParserFactory {
    @Override
    public RuleConfigParser createRuleParser() {
        return new JsonRuleConfigParser();
    }

    @Override
    public SystemConfigParser createSystemParser() {
        return new JsonSystemConfigParser();
    }
}
