package edu.univ.erp.data;

import edu.univ.erp.db.Sql;
import edu.univ.erp.domain.Section;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class SectionDAO {

    static final String SELECT = "SELECT s.section_id, s.course_id, c.code, c.title, c.credits, c.department, "
            + "s.instructor_id, COALESCE(NULLIF(u.full_name, ''), u.username) AS instructor_name, "
            + "s.day_time, s.room, s.capacity, s.enrolled_count, s.semester, s.year "
            + "FROM sections s JOIN courses c ON c.course_id = s.course_id "
            + "LEFT JOIN university_auth.users_auth u ON u.user_id = s.instructor_id";

    private SectionDAO() {
    }

    static Section map(ResultSet rs) throws SQLException {
        return new Section(rs.getInt("section_id"), rs.getInt("course_id"), rs.getString("code"),
                rs.getString("title"), rs.getInt("credits"), Sql.intOrNull(rs, "instructor_id"),
                rs.getString("instructor_name"), rs.getString("day_time"), rs.getString("room"),
                rs.getInt("capacity"), rs.getInt("enrolled_count"), rs.getString("semester"), rs.getInt("year"),
                rs.getString("department"));
    }

    public static List<Section> listAll(Connection c) throws SQLException {
        return Sql.list(c, SELECT + " ORDER BY s.year DESC, s.semester, c.code, s.section_id", SectionDAO::map);
    }

    public static List<Section> listByTerm(Connection c, String semester, int year) throws SQLException {
        return Sql.list(c, SELECT + " WHERE s.semester = ? AND s.year = ? ORDER BY c.code, s.section_id",
                SectionDAO::map, semester, year);
    }

    public static List<Section> listByInstructor(Connection c, int instructorId) throws SQLException {
        return Sql.list(c, SELECT + " WHERE s.instructor_id = ? ORDER BY s.year DESC, s.semester, c.code",
                SectionDAO::map, instructorId);
    }

    public static Optional<Section> find(Connection c, int id) throws SQLException {
        return Sql.one(c, SELECT + " WHERE s.section_id = ?", SectionDAO::map, id);
    }

    /** Locks the section row for the rest of the transaction so concurrent enrolments queue up. */
    public static void lock(Connection c, int id) throws SQLException {
        Sql.count(c, "SELECT section_id FROM sections WHERE section_id = ? FOR UPDATE", id);
    }

    public static int insert(Connection c, int courseId, Integer instructorId, String dayTime, String room,
                             int capacity, String semester, int year) throws SQLException {
        return Sql.insert(c, "INSERT INTO sections (course_id, instructor_id, day_time, room, capacity, semester, "
                + "year) VALUES (?, ?, ?, ?, ?, ?, ?)", courseId, instructorId, dayTime, room, capacity, semester, year);
    }

    public static void update(Connection c, int id, int courseId, Integer instructorId, String dayTime, String room,
                              int capacity, String semester, int year) throws SQLException {
        Sql.update(c, "UPDATE sections SET course_id = ?, instructor_id = ?, day_time = ?, room = ?, capacity = ?, "
                + "semester = ?, year = ? WHERE section_id = ?", courseId, instructorId, dayTime, room, capacity,
                semester, year, id);
    }

    /** Recomputes enrolled_count from the enrollments table (the single source of truth). */
    public static int recount(Connection c, int id) throws SQLException {
        Sql.update(c, "UPDATE sections SET enrolled_count = (SELECT COUNT(*) FROM enrollments "
                + "WHERE section_id = ? AND status = 'REGISTERED') WHERE section_id = ?", id, id);
        return Sql.count(c, "SELECT enrolled_count FROM sections WHERE section_id = ?", id);
    }

    public static int enrollmentCount(Connection c, int id) throws SQLException {
        return Sql.count(c, "SELECT COUNT(*) FROM enrollments WHERE section_id = ?", id);
    }

    public static void delete(Connection c, int id) throws SQLException {
        Sql.update(c, "DELETE FROM section_components WHERE section_id = ?", id);
        Sql.update(c, "DELETE FROM announcements WHERE section_id = ?", id);
        Sql.update(c, "DELETE FROM sections WHERE section_id = ?", id);
    }

    public static int countForInstructor(Connection c, int instructorId) throws SQLException {
        return Sql.count(c, "SELECT COUNT(*) FROM sections WHERE instructor_id = ?", instructorId);
    }

    public static List<String> terms(Connection c) throws SQLException {
        return Sql.list(c, "SELECT DISTINCT CONCAT(semester, ' ', year) t, year, semester FROM sections "
                + "ORDER BY year DESC, semester", rs -> rs.getString("t"));
    }
}
