package edu.univ.erp.data;

import edu.univ.erp.db.Sql;
import edu.univ.erp.domain.GradeComponent;
import edu.univ.erp.domain.GradeEntry;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GradeDAO {

    private GradeDAO() {
    }

    static GradeEntry map(ResultSet rs) throws SQLException {
        return new GradeEntry(rs.getInt("enrollment_id"), rs.getString("component"), rs.getDouble("score"),
                rs.getDouble("max_score"), rs.getDouble("weight"));
    }

    public static List<GradeComponent> components(Connection c, int sectionId) throws SQLException {
        return Sql.list(c, "SELECT component_id, section_id, name, max_score, weight, position "
                + "FROM section_components WHERE section_id = ? ORDER BY position, component_id",
                rs -> new GradeComponent(rs.getInt("component_id"), rs.getInt("section_id"), rs.getString("name"),
                        rs.getDouble("max_score"), rs.getDouble("weight"), rs.getInt("position")), sectionId);
    }

    public static void addDefaultComponents(Connection c, int sectionId) throws SQLException {
        Sql.update(c, "INSERT INTO section_components (section_id, name, max_score, weight, position) VALUES "
                + "(?, 'Quiz', 20, 20, 1), (?, 'Midterm', 30, 30, 2), (?, 'Final', 50, 50, 3)",
                sectionId, sectionId, sectionId);
    }

    public static void replaceComponents(Connection c, int sectionId, List<GradeComponent> comps)
            throws SQLException {
        Sql.update(c, "DELETE FROM section_components WHERE section_id = ?", sectionId);
        int pos = 1;
        for (GradeComponent g : comps) {
            Sql.update(c, "INSERT INTO section_components (section_id, name, max_score, weight, position) "
                    + "VALUES (?, ?, ?, ?, ?)", sectionId, g.name(), g.maxScore(), g.weight(), pos++);
        }
    }

    /** Scores for every enrollment in a section, keyed by enrollment id then component name. */
    public static Map<Integer, Map<String, GradeEntry>> forSection(Connection c, int sectionId)
            throws SQLException {
        Map<Integer, Map<String, GradeEntry>> out = new HashMap<>();
        Sql.list(c, "SELECT g.enrollment_id, g.component, g.score, g.max_score, g.weight FROM grades g "
                + "JOIN enrollments e ON e.enrollment_id = g.enrollment_id WHERE e.section_id = ?", rs -> {
                    GradeEntry g = map(rs);
                    out.computeIfAbsent(g.enrollmentId(), k -> new HashMap<>()).put(g.component(), g);
                    return g;
                }, sectionId);
        return out;
    }

    public static List<GradeEntry> forEnrollment(Connection c, int enrollmentId) throws SQLException {
        return Sql.list(c, "SELECT enrollment_id, component, score, max_score, weight FROM grades "
                + "WHERE enrollment_id = ? ORDER BY grade_id", GradeDAO::map, enrollmentId);
    }

    public static void upsert(Connection c, int enrollmentId, String component, double score, double max,
                              double weight) throws SQLException {
        Sql.update(c, "INSERT INTO grades (enrollment_id, component, score, max_score, weight) "
                + "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE score = VALUES(score), "
                + "max_score = VALUES(max_score), weight = VALUES(weight)", enrollmentId, component, score, max, weight);
    }

    public static void delete(Connection c, int enrollmentId, String component) throws SQLException {
        Sql.update(c, "DELETE FROM grades WHERE enrollment_id = ? AND component = ?", enrollmentId, component);
    }

    /** Drops scores for components that no longer exist in the section's scheme. */
    public static void deleteOrphans(Connection c, int sectionId) throws SQLException {
        Sql.update(c, "DELETE g FROM grades g JOIN enrollments e ON e.enrollment_id = g.enrollment_id "
                + "WHERE e.section_id = ? AND g.component NOT IN "
                + "(SELECT name FROM section_components WHERE section_id = ?)", sectionId, sectionId);
    }
}
