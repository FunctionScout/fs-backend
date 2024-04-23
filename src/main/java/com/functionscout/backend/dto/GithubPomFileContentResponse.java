package com.functionscout.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GithubPomFileContentResponse {

    @JsonProperty("name")
    private String name;

    @JsonProperty("content")
    private String content;
}
