package com.functionscout.backend.repository;

import com.functionscout.backend.model.Dependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DependencyRepository extends JpaRepository<Dependency, Integer> {
}
