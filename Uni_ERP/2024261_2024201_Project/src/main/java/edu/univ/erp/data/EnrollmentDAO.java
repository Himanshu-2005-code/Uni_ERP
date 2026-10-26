package edu.univ.erp.data;

import edu.univ.erp.db.Sql;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.RosterEntry;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class EnrollmentDAO {

    private static final String SELECT = "SELECT e.enrollment_id, e.student_id, e.status, e.enrollment_date, "
            + "e.final_grade, e.final_percentage, x.* FROM enrollments e "
            + "JOIN (" + SectionDAO.SELECT + ") x ON x.section_id = e.section_id";

    private EnrollmentDAO() {
    }

    static Enrollment map(ResultSet rs) throws SQLException {
        return new Enrollment(rs.getInt("enrollment_id"), rs.getInt("student_id"), rs.getInt("section_id"),
                rs.getString("status"), Sql.time(rs, "enrollment_date"), rs.getString("final_grade"),
                Sql.doubleOrNull(rs, "final_percentage"), SectionDAO.map(rs));
    }

    /** All of a student's enrollments (any status), newest term first. */
    public static List<Enrollment> forStudent(Connection c, int studentId) throws SQLException {
        return Sql.list(c, SELECT + " WHERE e.student_id = ? ORDER BY x.year DESC, x.semester, x.code",
                EnrollmentDAO::map, studentId);
    }

    public static Optional<Enrollment> find(Connection c, int id) throws SQLException {
        return Sql.one(c, SELECT + " WHERE e.enrollment_id = ?", EnrollmentDAO::map, id);
    }

    public static Optional<Enrollment> find(Connection c, int studentId, int sectionId) throws SQLException {
        return Sql.one(c, SELECT + " WHERE e.student_id = ? AND e.section_id = ?", EnrollmentDAO::map,
                studentId, sectionId);
    }

    public static int insert(Connection c, int studentId, int sectionId) throws SQLException {
        return Sql.insert(c, "INSERT INTO enrollments (student_id, section_id, status, enrollment_date) "
                + "VALUES (?, ?, 'REGISTERED', NOW())", studentId, sectionId);
    }

    /** Re-registers a previously dropped enrollment, clearing any stale marks. */
    public static void reactivate(Connection c, int enrollmentId) throws SQLException {
        Sql.update(c, "DELETE FROM grades WHERE enrollment_id = ?", enrollmentId);
        Sql.update(c, "UPDATE enrollments SET status = 'REGISTERED', enrollment_date = NOW(), final_grade = NULL, "
                + "final_percentage = NULL WHERE enrollment_id = ?", enrollmentId);
    }

    public static void setStatus(Connection c, int enrollmentId, String status) throws SQLException {
        Sql.update(c, "UPDATE enrollments SET status = ? WHERE enrollment_id = ?", status, enrollmentId);
    }

    public static void setFinal(Connection c, int enrollmentId, String grade, Double percent) throws SQLException {
        Sql.update(c, "UPDATE enrollments SET final_grade = ?, final_percentage = ? WHERE enrollment_id = ?",
                grade, percent, enrollmentId);
    }

    public static List<RosterEntry> roster(Connection c, int sectionId) throws SQLException {
        return Sql.list(c, "SELECT e.enrollment_id, e.student_id, s.roll_no, "
                + "COALESCE(NULLIF(u.full_name, ''), u.username) name, s.program, e.final_grade, e.final_percentage "
                + "FROM enrollments e JOIN students s ON s.user_id = e.student_id "
                + "JOIN university_auth.users_auth u ON u.user_id = e.student_id "
                + "WHERE e.section_id = ? AND e.status IN ('REGISTERED', 'COMPLETED') ORDER BY s.roll_no",
                rs -> new RosterEntry(rs.getInt("enrollment_id"), rs.getInt("student_id"), rs.getString("roll_no"),
                        rs.getString("name"), rs.getString("program"), rs.getString("final_grade"),
                        Sql.doubleOrNull(rs, "final_percentage")), sectionId);
    }

    public static int countForStudent(Connection c, int studentId) throws SQLException {
        return Sql.count(c, "SELECT COUNT(*) FROM enrollments WHERE student_id = ?", studentId);
    }

    /** Removes every enrollment (and its grades) for a student. */
    public static void deleteForStudent(Connection c, int studentId) throws SQLException {
        Sql.update(c, "DELETE g FROM grades g JOIN enrollments e ON e.enrollment_id = g.enrollment_id "
                + "WHERE e.student_id = ?", studentId);
        Sql.update(c, "DELETE FROM enrollments WHERE student_id = ?", studentId);
    }
}
