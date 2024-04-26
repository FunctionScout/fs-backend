package com.functionscout.backend.dto;

import com.functionscout.backend.model.Dependency;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WebServiceResponse {
    private String githubUrl;
    private List<WebServiceDependencyResponse> dependencies;
}
