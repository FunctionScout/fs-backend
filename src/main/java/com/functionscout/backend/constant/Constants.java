package com.functionscout.backend.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Constants {

    public static final String GITHUB_ISSUE_TITLE = "FunctionScout: Function signature mismatch";
    public static final String GITHUB_ISSUE_BODY_TEMPLATE = """
            Mismatching function details:\s
            Function Name: %s
            Function Signature: %s
            Dependency: %s
            """;
}
