package com.functionscout.backend.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum DependencyType {

    INTERNAL(0),
    EXTERNAL(1);

    private final short type;

    private static final Map<Short, DependencyType> LOOKUP = EnumSet.allOf(DependencyType.class).stream()
            .collect(Collectors.toMap(DependencyType::getType, Function.identity()));

    DependencyType(final int type) {
        this.type = (short) type;
    }

    public static DependencyType getDependencyType(short type) {
        if (!LOOKUP.containsKey(type)) {
            throw new IllegalArgumentException("Invalid type: " + type);
        }

        return LOOKUP.get(type);
    }

    public short getType() {
        return type;
    }
}
