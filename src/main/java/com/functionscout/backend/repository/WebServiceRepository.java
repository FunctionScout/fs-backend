package com.functionscout.backend.repository;

import com.functionscout.backend.model.WebService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebServiceRepository extends JpaRepository<WebService, Integer> {

    Optional<WebService> findWebServiceByUuid(String serviceId);

    @Query(value = "select count(ws) from WebService ws " +
            "where ws.githubUrl = :githubUrl and ws.status in (:statuses)")
    int countByGithubUrlAndStatuses(String githubUrl, List<Short> statuses);
}
