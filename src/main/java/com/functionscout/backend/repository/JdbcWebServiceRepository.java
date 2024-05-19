package com.functionscout.backend.repository;

import com.functionscout.backend.model.WebService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

@Repository
public class JdbcWebServiceRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<WebService> findAllByDependencyId(final Integer dependencyId) {
        final String query = "select * from WebService ws " +
                "inner join WebServiceDependency wsd on wsd.serviceId = ws.id " +
                "where ws.status = 1 and wsd.dependencyId = " + dependencyId;

        return namedParameterJdbcTemplate.query(
                query,
                new HashMap<>(),
                new WebServiceMapper()
        );
    }

    private static final class WebServiceMapper implements RowMapper<WebService> {
        public WebService mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final WebService webService = new WebService();
            webService.setId(rs.getInt("id"));
            webService.setUuid(rs.getString("uuid"));
            webService.setGithubUrl(rs.getString("githubUrl"));
            webService.setName(rs.getString("name"));
            webService.setStatus(rs.getShort("status"));
            webService.setMessage(rs.getString("message"));
            webService.setUniqueHash(rs.getString("uniqueHash"));
            webService.setCreateDT(rs.getTimestamp("createDT"));
            webService.setUpdateDT(rs.getTimestamp("updateDT"));

            return webService;
        }
    }
}
