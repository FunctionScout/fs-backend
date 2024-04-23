package com.functionscout.backend.repository;

import com.functionscout.backend.model.WebService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebServiceRepository extends JpaRepository<WebService, Integer> {

    Optional<WebService> findWebServiceByUuid(String serviceId);

    boolean existsByGithubUrl(String githubUrl);
}
