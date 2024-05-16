package com.functionscout.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsedFunctionDependencyFromDB {

    private Integer webServiceDependencyId;
    private Integer functionId;
}
