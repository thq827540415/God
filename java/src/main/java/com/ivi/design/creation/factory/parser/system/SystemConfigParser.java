package com.ivi.design.creation.factory.parser.system;

import com.ivi.design.creation.factory.RuleConfigSource;

public interface SystemConfigParser {
    RuleConfigSource.RuleConfig parse(String configText);
}
