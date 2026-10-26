package edu.univ.erp.data;

import edu.univ.erp.db.Sql;
import edu.univ.erp.domain.Course;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class CourseDAO {

    private static final String COLS = "course_id, code, title, credits, description, department";

    private CourseDAO() {
    }

    static Course map(ResultSet rs) throws SQLException {
        return new Course(rs.getInt("course_id"), rs.getString("code"), rs.getString("title"),
                rs.getInt("credits"), rs.getString("description"), rs.getString("department"));
    }

    public static List<Course> listAll(Connection c) throws SQLException {
        return Sql.list(c, "SELECT " + COLS + " FROM courses ORDER BY code", CourseDAO::map);
    }

    public static Optional<Course> find(Connection c, int id) throws SQLException {
        return Sql.one(c, "SELECT " + COLS + " FROM courses WHERE course_id = ?", CourseDAO::map, id);
    }

    public static boolean codeExists(Connection c, String code, int exceptId) throws SQLException {
        return Sql.count(c, "SELECT COUNT(*) FROM courses WHERE code = ? AND course_id <> ?", code, exceptId) > 0;
    }

    public static int insert(Connection c, String code, String title, int credits, String description,
                             String department) throws SQLException {
        return Sql.insert(c, "INSERT INTO courses (code, title, credits, description, department) "
                + "VALUES (?, ?, ?, ?, ?)", code, title, credits, description, department);
    }

    public static void update(Connection c, int id, String code, String title, int credits, String description,
                              String department) throws SQLException {
        Sql.update(c, "UPDATE courses SET code = ?, title = ?, credits = ?, description = ?, department = ? "
                + "WHERE course_id = ?", code, title, credits, description, department, id);
    }

    public static int sectionCount(Connection c, int id) throws SQLException {
        return Sql.count(c, "SELECT COUNT(*) FROM sections WHERE course_id = ?", id);
    }

    public static void delete(Connection c, int id) throws SQLException {
        Sql.update(c, "DELETE FROM courses WHERE course_id = ?", id);
    }
}
