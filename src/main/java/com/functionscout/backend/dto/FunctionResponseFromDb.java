package com.functionscout.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FunctionResponseFromDb {

    private Integer id;
    private String name;
    private String signature;
    private String returnType;
    private Integer classId;
}
