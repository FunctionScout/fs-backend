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
public class DependencyResponseDTO {

    private String name;
    private String version;
    private String type;
    private List<FunctionResponseDTO> usedFunctions;
}
