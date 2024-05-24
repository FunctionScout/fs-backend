package com.functionscout.backend.repository;

import com.functionscout.backend.model.WebService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WebServiceRepository extends JpaRepository<WebService, Integer> {

    Optional<WebService> findWebServiceByUuid(String serviceId);

    Optional<WebService> findWebServiceByGithubUrlAndStatus(String githubUrl, Short status);

    @Query(value = "select count(ws) from WebService ws " +
            "where ws.githubUrl = :githubUrl and ws.status = :status")
    int countByGithubUrlAndStatus(String githubUrl, Short status);
}
