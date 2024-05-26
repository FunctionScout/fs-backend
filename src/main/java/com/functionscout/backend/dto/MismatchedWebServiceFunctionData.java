package com.functionscout.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MismatchedWebServiceFunctionData {

    private String githubUrl;
    private String dependencyName;
    private String functionName;
    private String functionSignature;
    private String functionReturnType;

    @Override
    public String toString() {
        return "MismatchedWebServiceFunctionData{" +
                "githubUrl='" + githubUrl + '\'' +
                ", dependencyName='" + dependencyName + '\'' +
                ", functionName='" + functionName + '\'' +
                ", functionSignature='" + functionSignature + '\'' +
                ", functionReturnType='" + functionReturnType + '\'' +
                '}';
    }
}
