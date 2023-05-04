package com.ivi.design.creation.factory.parser.rule;

import com.ivi.design.creation.factory.RuleConfigSource;

public interface RuleConfigParser {
    RuleConfigSource.RuleConfig parse(String configText);
}
