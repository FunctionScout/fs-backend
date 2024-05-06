package com.functionscout.backend.dto;

import com.functionscout.backend.model.Function;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FunctionDetailResponseDTO extends FunctionResponseDTO {
    private String signature;

    public FunctionDetailResponseDTO(final Function function) {
        setId(function.getUuid());
        setName(function.getName());
        setSignature(function.getSignature());
        setClazz(function.getClazz().getName());
    }
}
