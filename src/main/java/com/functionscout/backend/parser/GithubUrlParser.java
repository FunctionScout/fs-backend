package com.functionscout.backend.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class GithubUrlParser {

    @Value("${functionscout.github.url-pattern}")
    private String githubUrlPattern;

    public boolean isValid(final String githubUrl) {
        final Pattern pattern = Pattern.compile(githubUrlPattern);
        final Matcher matcher = pattern.matcher(githubUrl);

        return matcher.matches();
    }

    public List<String> parse(final String githubUrl) {
        final Pattern pattern = Pattern.compile(githubUrlPattern);
        final Matcher matcher = pattern.matcher(githubUrl);

        if (!matcher.matches()) {
            log.error("Unable to parse github url: " + githubUrl);
            return List.of("", "");
        }

        return List.of(matcher.group(2), matcher.group(3));
    }
}
