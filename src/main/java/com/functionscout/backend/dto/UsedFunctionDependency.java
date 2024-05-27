package com.functionscout.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsedFunctionDependency {

    private Integer webServiceDependencyId;
    private String className;
    private String signature;
}
