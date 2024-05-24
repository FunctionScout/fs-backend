package com.functionscout.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassData {

    private Integer id;
    private String name;
    private Integer serviceId;

    @Autowired
    public ClassData(final String name, final Integer serviceId) {
        this.name = name;
        this.serviceId = serviceId;
    }
}
