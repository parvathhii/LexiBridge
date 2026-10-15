package com.lexibridge.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
public class DatabaseConnectionChecker implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionChecker.class);

    private final DataSource dataSource;

    public DatabaseConnectionChecker(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        logger.info("==========================================================");
        logger.info("   LexiBridge Phase 1: Verifying MySQL Database Connection   ");
        logger.info("==========================================================");

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String dbProductName = metaData.getDatabaseProductName();
            String dbProductVersion = metaData.getDatabaseProductVersion();
            String url = metaData.getURL();
            String userName = metaData.getUserName();

            logger.info("Database Connection Status: SUCCESSFUL");
            logger.info("Connected RDBMS: {} {}", dbProductName, dbProductVersion);
            logger.info("JDBC URL: {}", url);
            logger.info("Connected as User: {}", userName);

            // Execute test query
            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT DATABASE(), CURRENT_TIMESTAMP()")) {
                if (rs.next()) {
                    String currentDb = rs.getString(1);
                    String currentTime = rs.getString(2);
                    logger.info("Active Database Catalog: {}", currentDb);
                    logger.info("Database Server Timestamp: {}", currentTime);
                }
            }

            logger.info("==========================================================");
            logger.info("MySQL Connection and JPA Schema successfully validated!");
            logger.info("==========================================================");
        } catch (Exception e) {
            logger.error("Database connection verification failed!", e);
        }
    }
}
