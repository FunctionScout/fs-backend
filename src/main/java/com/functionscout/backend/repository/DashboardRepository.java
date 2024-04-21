package com.functionscout.backend.repository;

import com.functionscout.backend.model.Component;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DashboardRepository extends JpaRepository<Component, Integer> {

    Optional<Component> findComponentByName(String name);
}
