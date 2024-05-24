package com.functionscout.backend.repository;

import com.functionscout.backend.model.WebServiceFunctionDependency;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcWebServiceFunctionDependencyRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void saveAll(final List<WebServiceFunctionDependency> webServiceFunctionDependencies) {
        final String insertQuery = """
                INSERT IGNORE INTO WebServiceFunctionDependency (dependentServiceId, dependingServiceId, functionId)
                VALUES
                (:dependentServiceId, :dependingServiceId, :functionId)
                """;
        final SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(webServiceFunctionDependencies);
        namedParameterJdbcTemplate.batchUpdate(insertQuery, batch);
    }
}
