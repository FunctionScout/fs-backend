package com.functionscout.backend.repository;

import com.functionscout.backend.dto.FunctionData;
import com.functionscout.backend.dto.FunctionResponseDTO;
import com.functionscout.backend.dto.FunctionResponseFromDb;
import com.functionscout.backend.dto.UsedFunctionDependency;
import com.functionscout.backend.dto.UsedFunctionDependencyFromDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class JdbcFunctionRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<FunctionResponseDTO> findAllUsedFunctionsByServiceId(final Integer serviceId) {
        final String query = "select f.uuid as functionId, f.name as functionName, c.name as className " +
                "from `Function` f " +
                "inner join Class c on f.classId = c.id " +
                "where f.isUsed = true and c.serviceId = " + serviceId;

        return namedParameterJdbcTemplate.query(
                query,
                new HashMap<>(),
                new FunctionResponseMapper()
        );
    }

    public List<UsedFunctionDependencyFromDB> findAllFunctionsByServiceIdAndSignature(final List<UsedFunctionDependency> usedFunctionDependencies) {
        if (usedFunctionDependencies.isEmpty()) {
            return new ArrayList<>();
        }

        final String query = "select f.id as functionId, d.id as webServiceDependencyId " +
                "from `Function` f " +
                "inner join Class c on f.classId = c.id " +
                "inner join WebService ws on ws.id = c.serviceId " +
                "inner join Dependency d on d.name = ws.name " +
                "where ";
        final String whereClause = "(c.serviceId = %s and c.name = '%s' and f.signature = '%s')";
        final StringBuilder whereClauseBuilder = new StringBuilder();
        int usedFunctionDependenciesSize = usedFunctionDependencies.size();

        for (UsedFunctionDependency usedFunctionDependency : usedFunctionDependencies) {
            if (usedFunctionDependency.getWebServiceDependencyId() != null) {
                whereClauseBuilder.append(String.format(
                        whereClause,
                        usedFunctionDependency.getWebServiceDependencyId(),
                        usedFunctionDependency.getClassName(),
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

    public void saveAll(final List<FunctionData> functionDataList) {
        final String insertQuery = """
                INSERT IGNORE `Function` (uuid, classId, name, signature, returnType, isUsed)
                VALUES
                (:uuid, :classId, :name, :signature, :returnType, false)
                """;
        final SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(functionDataList);
        namedParameterJdbcTemplate.batchUpdate(insertQuery, batch);
    }

    public void updateFunctionSignatures(final List<FunctionData> mismatchedFunctions) {
        final String query = "update `Function` set signature = :newSignature where uuid = :uuid";
        final Map<String, String>[] parameterMapArray = new Map[mismatchedFunctions.size()];

        for (int index = 0; index < mismatchedFunctions.size(); index++) {
            parameterMapArray[index] = Map.of(
                    "newSignature", mismatchedFunctions.get(index).getSignature(),
                    "uuid", mismatchedFunctions.get(index).getUuid()
            );
        }

        namedParameterJdbcTemplate.batchUpdate(query, parameterMapArray);
    }

    public List<FunctionResponseFromDb> findAllByServiceId(Integer serviceId) {
        final String query = "select f.id, f.uuid, f.name, f.signature, f.returnType, f.classId from " +
                "Class c " +
                "inner join `Function` f on f.classId = c.id " +
                "where c.serviceId = " + serviceId;

        return namedParameterJdbcTemplate.query(
                query,
                new HashMap<>(),
                new FunctionResponseFromDbMapper()
        );
    }

    public void updateFunctionsAsUsed(final List<Integer> functionIds) {
        final String query = "update `Function` set isUsed = true where id = :id";
        final Map<String, Integer>[] parameterMapArray = new Map[functionIds.size()];

        for (int index = 0; index < functionIds.size(); index++) {
            parameterMapArray[index] = Map.of(
                    "id", functionIds.get(index)
            );
        }

        namedParameterJdbcTemplate.batchUpdate(query, parameterMapArray);
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

    private static final class FunctionResponseFromDbMapper implements RowMapper<FunctionResponseFromDb> {
        public FunctionResponseFromDb mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final FunctionResponseFromDb functionResponseFromDb = new FunctionResponseFromDb();

            functionResponseFromDb.setId(rs.getInt("id"));
            functionResponseFromDb.setUuid(rs.getString("uuid"));
            functionResponseFromDb.setName(rs.getString("name"));
            functionResponseFromDb.setSignature(rs.getString("signature"));
            functionResponseFromDb.setReturnType(rs.getString("returnType"));
            functionResponseFromDb.setClassId(rs.getInt("classId"));

            return functionResponseFromDb;
        }
    }
}
