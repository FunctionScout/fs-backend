package com.functionscout.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class WebServiceFunctionDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer dependentServiceId;

    private Integer dependingServiceId;

    private Integer functionId;

    @Autowired
    public WebServiceFunctionDependency(final Integer dependentServiceId,
                                        final Integer dependingServiceId,
                                        final Integer functionId) {
        this.dependentServiceId = dependentServiceId;
        this.dependingServiceId = dependingServiceId;
        this.functionId = functionId;
    }
}
