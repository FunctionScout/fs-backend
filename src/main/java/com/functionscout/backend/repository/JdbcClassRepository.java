package com.functionscout.backend.repository;

import com.functionscout.backend.dto.WebServiceClassData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

@Repository
public class JdbcClassRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<WebServiceClassData> findAll() {
        return namedParameterJdbcTemplate.query(
                "select serviceId, name from Class",
                new HashMap<>(),
                new WebServiceClassMapper()
        );
    }

    private static final class WebServiceClassMapper implements RowMapper<WebServiceClassData> {
        public WebServiceClassData mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final WebServiceClassData webServiceClassData = new WebServiceClassData();
            webServiceClassData.setServiceId(rs.getInt("serviceId"));
            webServiceClassData.setClassName(rs.getString("name"));

            return webServiceClassData;
        }
    }
}
