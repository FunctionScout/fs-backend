package com.functionscout.backend.repository;

import com.functionscout.backend.dto.FunctionResponseDTO;
import com.functionscout.backend.dto.UsedFunctionDependency;
import com.functionscout.backend.dto.UsedFunctionDependencyFromDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Repository
public class JdbcFunctionRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<FunctionResponseDTO> findAllFunctionsByServiceId(final Integer serviceId) {
        final String query = "select f.uuid as functionId, f.name as functionName, c.name as className " +
                "from `Function` f " +
                "inner join Class c on f.classId = c.id " +
                "where c.serviceId = " + serviceId;

        return namedParameterJdbcTemplate.query(
                query,
                new HashMap<>(),
                new FunctionResponseMapper()
        );
    }

    public List<UsedFunctionDependencyFromDB> findAllFunctionsByServiceIdAndFunctionId(final List<UsedFunctionDependency> usedFunctionDependencies) {
        if (usedFunctionDependencies.isEmpty()) {
            return new ArrayList<>();
        }

        final String query = "select f.id as functionId, c.serviceId as webServiceDependencyId " +
                "from `Function` f " +
                "inner join Class c on f.classId = c.id " +
                "where ";
        final String whereClause = "(c.serviceId = %s and f.signature = '%s')";
        final StringBuilder whereClauseBuilder = new StringBuilder();
        int usedFunctionDependenciesSize = usedFunctionDependencies.size();

        for (UsedFunctionDependency usedFunctionDependency : usedFunctionDependencies) {
            if (usedFunctionDependency.getWebServiceDependencyId() != null) {
                whereClauseBuilder.append(String.format(
                        whereClause,
                        usedFunctionDependency.getWebServiceDependencyId(),
                        usedFunctionDependency.getSignature())
                );

                if (usedFunctionDependenciesSize > 1) {
                    whereClauseBuilder.append(" OR ");
                }
            }

            usedFunctionDependenciesSize--;
        }

        return namedParameterJdbcTemplate.query(
                query + whereClauseBuilder,
                new HashMap<>(),
                new UsedFunctionDependencyMapper()
        );
    }

    private static final class FunctionResponseMapper implements RowMapper<FunctionResponseDTO> {
        public FunctionResponseDTO mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final FunctionResponseDTO functionResponseDTO = new FunctionResponseDTO();
            functionResponseDTO.setId(rs.getString("functionId"));
            functionResponseDTO.setName(rs.getString("functionName"));
            functionResponseDTO.setClazz(rs.getString("className"));

            return functionResponseDTO;
        }
    }

    private static final class UsedFunctionDependencyMapper implements RowMapper<UsedFunctionDependencyFromDB> {
        public UsedFunctionDependencyFromDB mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final UsedFunctionDependencyFromDB usedFunctionDependencyFromDB = new UsedFunctionDependencyFromDB();
            usedFunctionDependencyFromDB.setWebServiceDependencyId(rs.getInt("webServiceDependencyId"));
            usedFunctionDependencyFromDB.setFunctionId(rs.getInt("functionId"));

            return usedFunctionDependencyFromDB;
        }
    }
}
