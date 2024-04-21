package com.functionscout.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponseDTO {
    private String name;
    private String githubUrl;
    private List<Dependency> dependencies;
}
