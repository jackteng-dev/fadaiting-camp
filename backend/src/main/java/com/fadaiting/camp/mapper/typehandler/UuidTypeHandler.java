package com.fadaiting.camp.mapper.typehandler;

import org.apache.ibatis.type.*;
import java.sql.*;
import java.util.UUID;

@MappedTypes(UUID.class)
@MappedJdbcTypes(value = JdbcType.OTHER, includeNullJdbcType = true)
public class UuidTypeHandler extends BaseTypeHandler<UUID> {
    @Override public void setNonNullParameter(PreparedStatement statement, int index, UUID value, JdbcType jdbcType) throws SQLException {
        statement.setObject(index, value, Types.OTHER);
    }
    @Override public UUID getNullableResult(ResultSet result, String column) throws SQLException { return result.getObject(column, UUID.class); }
    @Override public UUID getNullableResult(ResultSet result, int column) throws SQLException { return result.getObject(column, UUID.class); }
    @Override public UUID getNullableResult(CallableStatement statement, int column) throws SQLException { return statement.getObject(column, UUID.class); }
}
