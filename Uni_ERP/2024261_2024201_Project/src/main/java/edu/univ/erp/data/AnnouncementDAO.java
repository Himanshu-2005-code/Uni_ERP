package edu.univ.erp.data;

import edu.univ.erp.db.Sql;
import edu.univ.erp.domain.Announcement;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public final class AnnouncementDAO {

    private static final String SELECT = "SELECT a.announcement_id, a.author_id, a.section_id, a.title, a.body, "
            + "a.created_at, COALESCE(NULLIF(u.full_name, ''), u.username, 'Unknown') author_name, "
            + "CONCAT(c.code, ' · ', s.day_time) section_label FROM announcements a "
            + "LEFT JOIN university_auth.users_auth u ON u.user_id = a.author_id "
            + "LEFT JOIN sections s ON s.section_id = a.section_id "
            + "LEFT JOIN courses c ON c.course_id = s.course_id";

    private AnnouncementDAO() {
    }

    static Announcement map(ResultSet rs) throws SQLException {
        return new Announcement(rs.getInt("announcement_id"), rs.getInt("author_id"), rs.getString("author_name"),
                Sql.intOrNull(rs, "section_id"), rs.getString("section_label"), rs.getString("title"),
                rs.getString("body"), Sql.time(rs, "created_at"));
    }

    public static List<Announcement> all(Connection c, int limit) throws SQLException {
        return Sql.list(c, SELECT + " ORDER BY a.created_at DESC, a.announcement_id DESC LIMIT ?",
                AnnouncementDAO::map, limit);
    }

    /** Campus-wide notices plus those for sections the student is registered in. */
    public static List<Announcement> forStudent(Connection c, int studentId, int limit) throws SQLException {
        return Sql.list(c, SELECT + " WHERE a.section_id IS NULL OR a.section_id IN (SELECT section_id FROM "
                + "enrollments WHERE student_id = ? AND status = 'REGISTERED') "
                + "ORDER BY a.created_at DESC, a.announcement_id DESC LIMIT ?", AnnouncementDAO::map, studentId, limit);
    }

    /** Campus-wide notices plus those for the instructor's own sections. */
    public static List<Announcement> forInstructor(Connection c, int instructorId, int limit) throws SQLException {
        return Sql.list(c, SELECT + " WHERE a.section_id IS NULL OR s.instructor_id = ? "
                + "ORDER BY a.created_at DESC, a.announcement_id DESC LIMIT ?", AnnouncementDAO::map, instructorId,
                limit);
    }

    public static int insert(Connection c, int authorId, Integer sectionId, String title, String body)
            throws SQLException {
        return Sql.insert(c, "INSERT INTO announcements (author_id, section_id, title, body) VALUES (?, ?, ?, ?)",
                authorId, sectionId, title, body);
    }

    public static int authorOf(Connection c, int id) throws SQLException {
        return Sql.count(c, "SELECT author_id FROM announcements WHERE announcement_id = ?", id);
    }

    public static void delete(Connection c, int id) throws SQLException {
        Sql.update(c, "DELETE FROM announcements WHERE announcement_id = ?", id);
    }
}
