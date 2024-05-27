package com.functionscout.backend.dto;

import com.functionscout.backend.model.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FunctionDetailResponseDTO extends FunctionResponseDTO {
    private String signature;
    private List<DependentWebServicesDTO> dependentServices;

    public FunctionDetailResponseDTO(final Function function, final List<DependentWebServicesDTO> dependentServices) {
        setId(function.getUuid());
        setName(function.getName());
        setSignature(function.getSignature());
        setClazz(function.getClazz().getName());

        this.dependentServices = dependentServices;
    }
}
