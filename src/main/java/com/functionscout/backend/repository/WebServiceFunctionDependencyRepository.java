package com.functionscout.backend.repository;

import com.functionscout.backend.model.WebServiceFunctionDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebServiceFunctionDependencyRepository extends JpaRepository<WebServiceFunctionDependency, Integer> {
}
