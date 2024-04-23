package com.functionscout.backend.repository;

import com.functionscout.backend.dto.DependencyDTO;
import com.functionscout.backend.model.Dependency;
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
public class JdbcDependencyRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public static final String INSERT_DEPENDENCY_QUERY = """
            INSERT IGNORE INTO Dependency (name, version, type)
            VALUES
            (:name, :version, :type)""";

    public void saveAllDependencies(final List<DependencyDTO> dependencyDTOS) {
        final SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(dependencyDTOS);
        namedParameterJdbcTemplate.batchUpdate(INSERT_DEPENDENCY_QUERY, batch);
    }

    public List<Dependency> getDependencies(final List<DependencyDTO> dependencyDTOS) {
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
}
