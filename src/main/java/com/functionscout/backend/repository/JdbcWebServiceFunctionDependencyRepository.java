package com.functionscout.backend.repository;

import com.functionscout.backend.dto.MismatchedWebServiceFunctionData;
import com.functionscout.backend.model.WebServiceFunctionDependency;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
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

    public List<MismatchedWebServiceFunctionData> findAllDependentServicesForFunctions(final List<Integer> mismatchedFunctionIds) {
        final String query = "select ws.githubUrl, d.name as dependencyName, f.name as functionName, " +
                "f.signature as functionSignature, f.returnType as functionReturnType from " +
                "WebServiceFunctionDependency wsfd " +
                "inner join WebService ws on ws.id = wsfd.dependentServiceId " +
                "inner join `Function` f on f.id = wsfd.functionId " +
                "inner join Dependency d on d.id = wsfd.dependingServiceId " +
                "where wsfd.functionId in (";
        final StringBuilder queryBuilder = new StringBuilder();
        int mismatchedFunctionCount = mismatchedFunctionIds.size();

        for (Integer functionId : mismatchedFunctionIds) {
            queryBuilder.append(functionId);

            if (mismatchedFunctionCount > 1) {
                queryBuilder.append(", ");
            }
        }

        queryBuilder.append(")");

        return namedParameterJdbcTemplate.query(
                query + queryBuilder,
                new HashMap<>(),
                new MismatchedWebServiceFunctionDataMapper()
        );
    }

    private static final class MismatchedWebServiceFunctionDataMapper implements RowMapper<MismatchedWebServiceFunctionData> {
        public MismatchedWebServiceFunctionData mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final MismatchedWebServiceFunctionData mismatchedWebServiceFunctionData = new MismatchedWebServiceFunctionData();

            mismatchedWebServiceFunctionData.setGithubUrl(rs.getString("githubUrl"));
            mismatchedWebServiceFunctionData.setDependencyName(rs.getString("dependencyName"));
            mismatchedWebServiceFunctionData.setFunctionName(rs.getString("functionName"));
            mismatchedWebServiceFunctionData.setFunctionSignature(rs.getString("functionSignature"));
            mismatchedWebServiceFunctionData.setFunctionReturnType(rs.getString("functionReturnType"));

            return mismatchedWebServiceFunctionData;
        }
    }
}
