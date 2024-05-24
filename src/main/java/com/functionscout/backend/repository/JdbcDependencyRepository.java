package com.functionscout.backend.repository;

import com.functionscout.backend.dto.DependencyDTO;
import com.functionscout.backend.dto.DependencyData;
import com.functionscout.backend.dto.FunctionResponseDTO;
import com.functionscout.backend.dto.WebServiceDependencyDTO;
import com.functionscout.backend.dto.WebServiceFunctionDependencyDTO;
import com.functionscout.backend.enums.Status;
import com.functionscout.backend.model.Dependency;
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

@Repository
public class JdbcDependencyRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void saveAll(final List<DependencyDTO> dependencyDTOS) {
        final String insertQuery = """
                INSERT IGNORE INTO Dependency (name, version, type)
                VALUES
                (:name, :version, :type)""";
        final SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(dependencyDTOS);

        namedParameterJdbcTemplate.batchUpdate(insertQuery, batch);
    }

    public List<Dependency> findAllDependenciesByNameAndVersion(final List<DependencyDTO> dependencyDTOS) {
        String initialQuery = "select id, name, version, type, createDT, updateDT from Dependency where (";
        StringBuilder whereClause = new StringBuilder();
        String tempQuery = "(name = '%s' AND version = '%s')";

        for (DependencyDTO dependencyDTO : dependencyDTOS) {
            whereClause.append(String.format(tempQuery, dependencyDTO.getName(), dependencyDTO.getVersion()));

            if (!dependencyDTOS.get(dependencyDTOS.size() - 1).equals(dependencyDTO)) {
                whereClause.append(" OR ");
            }
        }

        return namedParameterJdbcTemplate.query(
                initialQuery + whereClause + ")",
                new HashMap<>(),
                new DependencyMapper()
        );
    }

    public List<WebServiceDependencyDTO> findAllWebServiceDependenciesByServiceId(final Integer serviceId) {
        final String query = "select ws.id as serviceId, d.id as dependencyId, d.name as name, " +
                "d.version as version, d.type as type from " +
                "WebService ws " +
                "inner join WebServiceDependency wsd on ws.id = wsd.serviceId " +
                "inner join Dependency d on d.id = wsd.dependencyId " +
                "where ws.id = " + serviceId;

        return namedParameterJdbcTemplate.query(
                query,
                new HashMap<>(),
                new WebServiceDependencyMapper()
        );
    }

    public List<WebServiceFunctionDependencyDTO> findAllUsedWebServiceFunctionDependencies(final List<WebServiceDependencyDTO> webServiceDependencies) {
        if (webServiceDependencies.isEmpty()) {
            return new ArrayList<>();
        }

        final String query = "select wsfd.dependentServiceId as serviceId, wsfd.dependingServiceId as dependencyId, " +
                "f.uuid as functionId, f.name as functionName, c.name as className from " +
                "WebServiceFunctionDependency wsfd " +
                "inner join `Function` f on f.id = wsfd.functionId " +
                "inner join Class c on c.id = f.classId " +
                "where ";
        final String whereClause = "(wsfd.dependentServiceId = %s and wsfd.dependingServiceId = '%s')";
        final StringBuilder whereClauseBuilder = new StringBuilder();
        int webServiceDependencyResponseCount = webServiceDependencies.size();

        for (WebServiceDependencyDTO webServiceDependencyDTO : webServiceDependencies) {
            whereClauseBuilder.append(String.format(
                    whereClause,
                    webServiceDependencyDTO.getServiceId(),
                    webServiceDependencyDTO.getDependencyId()
            ));

            if (webServiceDependencyResponseCount > 1) {
                whereClauseBuilder.append(" OR ");
            }

            webServiceDependencyResponseCount--;
        }

        return namedParameterJdbcTemplate.query(
                query + whereClauseBuilder,
                new HashMap<>(),
                new WebServiceFunctionDependencyDTOMapper()
        );
    }

    private static final class DependencyMapper implements RowMapper<Dependency> {
        public Dependency mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final Dependency dependency = new Dependency();
            dependency.setId(rs.getInt("id"));
            dependency.setName(rs.getString("name"));
            dependency.setVersion(rs.getString("version"));
            dependency.setType(rs.getShort("type"));
            dependency.setCreateDT(rs.getTimestamp("createDT"));
            dependency.setUpdateDT(rs.getTimestamp("updateDT"));

            return dependency;
        }
    }

    private static final class WebServiceDependencyMapper implements RowMapper<WebServiceDependencyDTO> {
        public WebServiceDependencyDTO mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final WebServiceDependencyDTO webServiceDependencyDTO = new WebServiceDependencyDTO();
            webServiceDependencyDTO.setServiceId(rs.getInt("serviceId"));
            webServiceDependencyDTO.setDependencyId(rs.getInt("dependencyId"));
            webServiceDependencyDTO.setDependencyData(new DependencyData(
                    rs.getString("name"),
                    rs.getString("version"),
                    Status.getStatus(rs.getShort("type")).name()
            ));

            return webServiceDependencyDTO;
        }
    }

    private static final class WebServiceFunctionDependencyDTOMapper implements RowMapper<WebServiceFunctionDependencyDTO> {
        public WebServiceFunctionDependencyDTO mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final WebServiceFunctionDependencyDTO webServiceFunctionDependencyDTO = new WebServiceFunctionDependencyDTO();
            webServiceFunctionDependencyDTO.setServiceId(rs.getInt("serviceId"));
            webServiceFunctionDependencyDTO.setDependencyId(rs.getInt("dependencyId"));
            webServiceFunctionDependencyDTO.setFunctionResponseDTO(new FunctionResponseDTO(
                    rs.getString("functionId"),
                    rs.getString("functionName"),
                    rs.getString("className")
            ));

            return webServiceFunctionDependencyDTO;
        }
    }
}
