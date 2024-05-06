package com.functionscout.backend.repository;

import com.functionscout.backend.dto.FunctionResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
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

    private static final class FunctionResponseMapper implements RowMapper<FunctionResponseDTO> {
        public FunctionResponseDTO mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final FunctionResponseDTO functionResponseDTO = new FunctionResponseDTO();
            functionResponseDTO.setId(rs.getString("functionId"));
            functionResponseDTO.setName(rs.getString("functionName"));
            functionResponseDTO.setClazz(rs.getString("className"));

            return functionResponseDTO;
        }
    }
}
