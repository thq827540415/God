package com.ivi.grammar;

import java.util.Optional;

public class OptionalUtil {
    private OptionalUtil() {
    }

    static Optional<Integer> stringToInt(String str) {
        try {
            return Optional.of(Integer.parseInt(str));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
