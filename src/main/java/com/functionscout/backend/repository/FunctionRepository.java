package com.functionscout.backend.repository;

import com.functionscout.backend.model.Function;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FunctionRepository extends JpaRepository<Function, Integer> {

    Optional<Function> findFunctionByUuid(String functionId);
}
