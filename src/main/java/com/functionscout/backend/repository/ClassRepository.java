package com.functionscout.backend.repository;

import com.functionscout.backend.model.Class;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface ClassRepository extends JpaRepository<Class, Integer> {

}
