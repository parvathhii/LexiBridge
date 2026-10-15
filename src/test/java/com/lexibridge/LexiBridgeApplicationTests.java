package com.lexibridge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LexiBridgeApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() {
        assertNotNull(dataSource, "DataSource bean must be configured and not null");
    }

    @Test
    void testDatabaseConnection() throws Exception {
        assertNotNull(dataSource, "DataSource bean should not be null");

        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "Database connection should be established");
            assertFalse(connection.isClosed(), "Connection should be open");

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT 1")) {
                assertTrue(rs.next(), "Query 'SELECT 1' should return at least one row");
                assertEquals(1, rs.getInt(1), "Result should be 1");
            }
        }
    }
}
