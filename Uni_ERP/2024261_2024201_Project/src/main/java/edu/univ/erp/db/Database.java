package edu.univ.erp.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class Database {
    private static HikariDataSource authDataSource;
    private static HikariDataSource erpDataSource;

    static {
        initializeDataSources();
    }

    private static void initializeDataSources() {
        // Auth database configuration
        HikariConfig authConfig = new HikariConfig();
        authConfig.setJdbcUrl("jdbc:mysql://localhost:3306/university_auth?useSSL=false&serverTimezone=UTC");
        authConfig.setUsername("root");
        authConfig.setPassword("root1234"); // Your MySQL password here
        authConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        authConfig.setMaximumPoolSize(10);
        authConfig.setMinimumIdle(2);
        authConfig.setConnectionTimeout(30000);
        authConfig.setIdleTimeout(600000);
        authConfig.setMaxLifetime(1800000);

        authDataSource = new HikariDataSource(authConfig);

        // ERP database configuration
        HikariConfig erpConfig = new HikariConfig();
        erpConfig.setJdbcUrl("jdbc:mysql://localhost:3306/university_erp?useSSL=false&serverTimezone=UTC");
        erpConfig.setUsername("root");
        erpConfig.setPassword("root1234"); // Your MySQL password here
        erpConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        erpConfig.setMaximumPoolSize(10);
        erpConfig.setMinimumIdle(2);
        erpConfig.setConnectionTimeout(30000);
        erpConfig.setIdleTimeout(600000);
        erpConfig.setMaxLifetime(1800000);

        erpDataSource = new HikariDataSource(erpConfig);
    }

    public static Connection getAuthConnection() throws SQLException {
        return authDataSource.getConnection();
    }

    public static Connection getERPConnection() throws SQLException {
        return erpDataSource.getConnection();
    }

    public static void closeDataSources() {
        if (authDataSource != null) {
            authDataSource.close();
        }
        if (erpDataSource != null) {
            erpDataSource.close();
        }
    }
    public static void testConnections() {
        System.out.println("Testing database connections...");
        try (Connection authConn = getAuthConnection()) {
            System.out.println("✓ Auth database connection successful");
        } catch (SQLException e) {
            System.err.println("✗ Auth database connection failed: " + e.getMessage());
        }

        try (Connection erpConn = getERPConnection()) {
            System.out.println("✓ ERP database connection successful");
        } catch (SQLException e) {
            System.err.println("✗ ERP database connection failed: " + e.getMessage());
        }
    }
}