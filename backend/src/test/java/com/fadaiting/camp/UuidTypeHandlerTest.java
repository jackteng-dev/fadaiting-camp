package com.fadaiting.camp;

import com.fadaiting.camp.mapper.typehandler.UuidTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UuidTypeHandlerTest {
    private final UuidTypeHandler handler = new UuidTypeHandler();
    private final UUID key = UUID.fromString("c890ea1e-5d76-4c0e-8252-2e0b3191812e");
    @Test void bindsNativePostgresUuid() throws SQLException {
        PreparedStatement statement = mock(PreparedStatement.class);
        handler.setParameter(statement,1,key,JdbcType.OTHER);
        verify(statement).setObject(1,key,Types.OTHER);
    }
    @Test void readsUuidAndPreservesNull() throws SQLException {
        ResultSet result = mock(ResultSet.class);
        when(result.getObject("idempotency_key",UUID.class)).thenReturn(key);
        assertEquals(key,handler.getResult(result,"idempotency_key"));
        assertNull(handler.getResult(result,1));
    }
    @Test void mapperResolvesUuidForParametersAndAutomaticResults() {
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.getTypeHandlerRegistry().register(UuidTypeHandler.class);
        configuration.addMapper(com.fadaiting.camp.mapper.RefundMapper.class);
        assertInstanceOf(UuidTypeHandler.class,configuration.getTypeHandlerRegistry().getTypeHandler(UUID.class));
    }
}
