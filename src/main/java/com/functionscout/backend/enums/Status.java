package com.functionscout.backend.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum Status {

    IN_PROGRESS(0),
    SUCCESS(1),
    FAILED(2);

    private final short code;

    private static final Map<Short, Status> LOOKUP = EnumSet.allOf(Status.class).stream()
            .collect(Collectors.toMap(Status::getCode, Function.identity()));

    Status(final int code) {
        this.code = (short) code;
    }

    public static Status getStatus(short code) {
        if (!LOOKUP.containsKey(code)) {
            throw new IllegalArgumentException("Invalid status code: " + code);
        }

        return LOOKUP.get(code);
    }

    public short getCode() {
        return code;
    }
}
