package com.functionscout.backend.repository;

import com.functionscout.backend.model.Class;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;


@Repository
public interface ClassRepository extends JpaRepository<Class, Integer> {

    @Query("select c.name from Class c")
    Set<String> findAllNames();
}
