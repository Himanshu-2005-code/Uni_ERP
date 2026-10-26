package edu.univ.erp.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates the databases on first run and upgrades older installs in place.
 * Every step is idempotent, so it is safe to run on each start.
 */
public final class SchemaManager {

    private SchemaManager() {
    }

    public static void ensureSchema() throws SQLException {
        try (Connection c = Database.serverConnection()) {
            runScript(c, "/sql/auth_db_create.sql");
            runScript(c, "/sql/erp_db_create.sql");
            migrate(c);
        }
    }

    /** True when no user accounts exist yet (fresh install). */
    public static boolean isEmpty() {
        return Database.read(c -> {
            try (Statement s = c.createStatement();
                 ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM university_auth.users_auth")) {
                rs.next();
                return rs.getInt(1) == 0;
            }
        });
    }

    private static void migrate(Connection c) throws SQLException {
        // Columns added in v2 (older databases were created without them)
        addColumn(c, "university_auth", "users_auth", "full_name", "VARCHAR(100)");
        addColumn(c, "university_auth", "users_auth", "email", "VARCHAR(120)");
        addColumn(c, "university_erp", "courses", "department", "VARCHAR(100)");
        addColumn(c, "university_erp", "enrollments", "final_percentage", "DECIMAL(5,2)");

        // v1 seed data could contain duplicate (enrollment, component) rows; keep the first one
        if (!hasIndex(c, "university_erp", "grades", "unique_grade")) {
            exec(c, "DELETE g1 FROM university_erp.grades g1 JOIN university_erp.grades g2 "
                    + "ON g1.enrollment_id = g2.enrollment_id AND g1.component = g2.component "
                    + "AND g1.grade_id > g2.grade_id");
            exec(c, "ALTER TABLE university_erp.grades ADD UNIQUE KEY unique_grade (enrollment_id, component)");
        }

        // Data fixes from v1
        exec(c, "UPDATE university_erp.sections SET semester = 'Monsoon' WHERE semester = 'Monsson'");
        exec(c, "UPDATE university_erp.sections s SET enrolled_count = "
                + "(SELECT COUNT(*) FROM university_erp.enrollments e "
                + "WHERE e.section_id = s.section_id AND e.status = 'REGISTERED')");

        // Every section gets the standard assessment scheme unless it already has one
        exec(c, "INSERT INTO university_erp.section_components (section_id, name, max_score, weight, position) "
                + "SELECT s.section_id, d.name, d.max_score, d.weight, d.position FROM university_erp.sections s "
                + "CROSS JOIN (SELECT 'Quiz' AS name, 20 AS max_score, 20 AS weight, 1 AS position "
                + "UNION ALL SELECT 'Midterm', 30, 30, 2 UNION ALL SELECT 'Final', 50, 50, 3) d "
                + "WHERE NOT EXISTS (SELECT 1 FROM university_erp.section_components sc "
                + "WHERE sc.section_id = s.section_id)");
    }

    private static void addColumn(Connection c, String schema, String table, String column, String type)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            ps.setString(3, column);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) > 0) return;
            }
        }
        exec(c, "ALTER TABLE " + schema + "." + table + " ADD COLUMN " + column + " " + type);
    }

    private static boolean hasIndex(Connection c, String schema, String table, String index) throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.STATISTICS "
                + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND INDEX_NAME = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, table);
            ps.setString(3, index);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private static void exec(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.execute(sql);
        }
    }

    static void runScript(Connection c, String resource) throws SQLException {
        for (String stmt : readStatements(resource)) {
            exec(c, stmt);
        }
    }

    /** Splits a SQL script into statements, dropping comments. Statements end with ';' at end of line. */
    static List<String> readStatements(String resource) {
        List<String> out = new ArrayList<>();
        try (InputStream in = SchemaManager.class.getResourceAsStream(resource)) {
            if (in == null) throw new IllegalStateException("Missing resource " + resource);
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder cur = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("--")) continue;
                cur.append(line).append('\n');
                if (t.endsWith(";")) {
                    String s = cur.toString().trim();
                    out.add(s.substring(0, s.length() - 1));
                    cur.setLength(0);
                }
            }
            if (cur.toString().trim().length() > 0) out.add(cur.toString().trim());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return out;
    }
}
