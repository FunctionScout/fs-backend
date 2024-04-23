package com.functionscout.backend.repository;

import com.functionscout.backend.model.WebServiceStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface WebServiceStatusRepository extends JpaRepository<WebServiceStatus, Integer> {

    @Modifying
    @Transactional
    @Query("update WebServiceStatus wss " +
            "set wss.status = :status " +
            "where wss.id = :id and wss.githubUrl = :githubUrl")
    void updateWebServiceStatus(short status, Integer id, String githubUrl);
}
