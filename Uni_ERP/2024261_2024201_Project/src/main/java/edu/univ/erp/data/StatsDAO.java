package edu.univ.erp.data;

import edu.univ.erp.db.Sql;
import edu.univ.erp.domain.LabelValue;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Aggregates for the admin dashboard. */
public final class StatsDAO {

    private StatsDAO() {
    }

    public static int count(Connection c, String table) throws SQLException {
        return Sql.count(c, "SELECT COUNT(*) FROM " + table);
    }

    public static int activeEnrollments(Connection c) throws SQLException {
        return Sql.count(c, "SELECT COUNT(*) FROM enrollments WHERE status = 'REGISTERED'");
    }

    public static int sectionsInTerm(Connection c, String semester, int year) throws SQLException {
        return Sql.count(c, "SELECT COUNT(*) FROM sections WHERE semester = ? AND year = ?", semester, year);
    }

    public static double capacityUsed(Connection c, String semester, int year) throws SQLException {
        return Sql.number(c, "SELECT COALESCE(SUM(enrolled_count) / NULLIF(SUM(capacity), 0), 0) FROM sections "
                + "WHERE semester = ? AND year = ?", semester, year);
    }

    public static List<LabelValue> enrollmentsByCourse(Connection c, String semester, int year, int limit)
            throws SQLException {
        return Sql.list(c, "SELECT c.code, SUM(s.enrolled_count) n FROM sections s JOIN courses c "
                + "ON c.course_id = s.course_id WHERE s.semester = ? AND s.year = ? GROUP BY c.code "
                + "ORDER BY n DESC, c.code LIMIT ?", rs -> new LabelValue(rs.getString(1), rs.getDouble(2)),
                semester, year, limit);
    }

    public static List<LabelValue> gradeDistribution(Connection c) throws SQLException {
        return Sql.list(c, "SELECT final_grade, COUNT(*) FROM enrollments WHERE final_grade IS NOT NULL "
                + "GROUP BY final_grade", rs -> new LabelValue(rs.getString(1), rs.getDouble(2)));
    }

    public static List<LabelValue> studentsByProgram(Connection c) throws SQLException {
        return Sql.list(c, "SELECT COALESCE(program, 'Other'), COUNT(*) n FROM students GROUP BY program "
                + "ORDER BY n DESC", rs -> new LabelValue(rs.getString(1), rs.getDouble(2)));
    }

    /** Registrations per day for the last {@code days} days (missing days filled with zero). */
    public static List<LabelValue> registrationTrend(Connection c, int days) throws SQLException {
        Map<LocalDate, Double> m = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) m.put(today.minusDays(i), 0.0);
        Sql.list(c, "SELECT DATE(enrollment_date) d, COUNT(*) n FROM enrollments "
                + "WHERE enrollment_date >= ? GROUP BY DATE(enrollment_date)", rs -> {
                    LocalDate d = rs.getDate(1).toLocalDate();
                    if (m.containsKey(d)) m.put(d, rs.getDouble(2));
                    return d;
                }, today.minusDays(days - 1L).atStartOfDay());
        return m.entrySet().stream()
                .map(e -> new LabelValue(e.getKey().getDayOfMonth() + "/" + e.getKey().getMonthValue(), e.getValue()))
                .toList();
    }

    /** Active enrollments by course department (rows) and student program (columns), for the 3D chart. */
    public static List<Object[]> enrollmentsByDepartmentAndProgram(Connection c) throws SQLException {
        return Sql.list(c, "SELECT COALESCE(c.department, 'General') dept, COALESCE(st.program, 'Other') prog, "
                + "COUNT(*) n FROM enrollments e JOIN sections s ON s.section_id = e.section_id "
                + "JOIN courses c ON c.course_id = s.course_id JOIN students st ON st.user_id = e.student_id "
                + "WHERE e.status = 'REGISTERED' GROUP BY dept, prog",
                rs -> new Object[]{rs.getString(1), rs.getString(2), rs.getDouble(3)});
    }
}
