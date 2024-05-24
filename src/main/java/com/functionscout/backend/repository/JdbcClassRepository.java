package com.functionscout.backend.repository;

import com.functionscout.backend.dto.ClassData;
import com.functionscout.backend.dto.WebServiceClassData;
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
public class JdbcClassRepository {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<WebServiceClassData> findAllClassesOfWebServiceDependencies(final Integer webServiceId) {
        final String query = "select c.serviceId as serviceId, c.name as name from WebServiceDependency wsd " +
                "inner join Dependency d on d.id = wsd.dependencyId " +
                "inner join WebService ws on ws.name = d.name " +
                "inner join Class c on c.serviceId = ws.id " +
                "where ws.status = 1 and wsd.serviceId = " + webServiceId;

        return namedParameterJdbcTemplate.query(
                query,
                new HashMap<>(),
                new WebServiceClassMapper()
        );
    }

    public List<WebServiceClassData> findAllClassesForWebService(final Integer webServiceId) {
        final String query = "select serviceId, name from Class where serviceId = " + webServiceId;

        return namedParameterJdbcTemplate.query(
                query,
                new HashMap<>(),
                new WebServiceClassMapper()
        );
    }

    public List<ClassData> saveAll(final List<ClassData> classDataList,
                                   final Integer serviceId) {
        final String insertQuery = """
                INSERT IGNORE INTO Class (name, serviceId)
                VALUES
                (:name, :serviceId)
                """;
        final SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(classDataList);
        namedParameterJdbcTemplate.batchUpdate(insertQuery, batch);

        final StringBuilder queryBuilder = new StringBuilder(
                "SELECT id, name, serviceId FROM Class " +
                        " WHERE serviceId = " + serviceId + " AND (");
        final String selectWhereClause = " name = '%s' ";
        int classDataListSize = classDataList.size();

        for (ClassData classData : classDataList) {
            queryBuilder.append(String.format(selectWhereClause, classData.getName()));

            if (classDataListSize > 1) {
                queryBuilder.append(" OR ");
            }

            classDataListSize--;
        }

        queryBuilder.append(")");

        return namedParameterJdbcTemplate.query(
                queryBuilder.toString(),
                new HashMap<>(),
                new ClassDataMapper()
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

    private static final class ClassDataMapper implements RowMapper<ClassData> {
        public ClassData mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final ClassData classData = new ClassData();
            classData.setId(rs.getInt("id"));
            classData.setName(rs.getString("name"));
            classData.setServiceId(rs.getInt("serviceId"));

            return classData;
        }
    }
}
