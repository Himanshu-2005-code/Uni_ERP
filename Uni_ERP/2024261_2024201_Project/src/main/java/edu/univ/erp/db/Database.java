package edu.univ.erp.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import edu.univ.erp.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Single connection pool for the whole app.
 *
 * The pool's default schema is university_erp; login data lives in
 * university_auth and is always referenced with its schema prefix. Using one
 * pool means a single transaction can touch both schemas, so creating a user
 * and their profile is now all-or-nothing.
 */
public final class Database {

    private static volatile HikariDataSource pool;

    private Database() {
    }

    public static synchronized void start() {
        if (pool != null) return;
        AppConfig c = AppConfig.get();
        HikariConfig hc = new HikariConfig();
        hc.setPoolName("campus-erp");
        hc.setJdbcUrl(c.erpUrl());
        hc.setUsername(c.getUser());
        hc.setPassword(c.getPassword());
        hc.setMaximumPoolSize(8);
        hc.setMinimumIdle(1);
        hc.setConnectionTimeout(8000);
        hc.setInitializationFailTimeout(8000);
        pool = new HikariDataSource(hc);
    }

    public static synchronized void stop() {
        if (pool != null) {
            pool.close();
            pool = null;
        }
    }

    public static boolean isStarted() {
        return pool != null;
    }

    public static Connection connection() throws SQLException {
        if (pool == null) start();
        return pool.getConnection();
    }

    /** A plain connection to the server without selecting a schema (used before the schema exists). */
    public static Connection serverConnection() throws SQLException {
        AppConfig c = AppConfig.get();
        DriverManager.setLoginTimeout(8);
        return DriverManager.getConnection(c.serverUrl(), c.getUser(), c.getPassword());
    }

    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection c) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlAction {
        void run(Connection c) throws SQLException;
    }

    /** Runs read-only work on a pooled connection. */
    public static <T> T read(SqlWork<T> work) {
        try (Connection c = connection()) {
            return work.run(c);
        } catch (SQLException e) {
            throw DbErrors.wrap(e);
        }
    }

    /** Runs work in a transaction, committing on success and rolling back on any failure. */
    public static <T> T tx(SqlWork<T> work) {
        try (Connection c = connection()) {
            boolean auto = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(auto);
            }
        } catch (SQLException e) {
            throw DbErrors.wrap(e);
        }
    }

    public static void txRun(SqlAction action) {
        tx(c -> {
            action.run(c);
            return null;
        });
    }
}
