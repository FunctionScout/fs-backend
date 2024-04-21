package com.functionscout.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Dependency {
    private String name;
    private String type;
}
