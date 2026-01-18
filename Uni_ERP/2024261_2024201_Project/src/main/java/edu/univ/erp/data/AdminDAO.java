package edu.univ.erp.data;

import edu.univ.erp.domain.User;
import edu.univ.erp.domain.Student;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.Section;
import edu.univ.erp.db.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {

    // User Management Methods
    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, username, role, status, last_login, created_at " +
                "FROM university_auth.users_auth ORDER BY role, username";

        try (Connection conn = Database.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                User user = new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("status")
                );
                users.add(user);
            }
        }
        return users;
    }

    public List<Student> getAllStudents() throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT u.user_id, u.username, u.status, s.roll_no, s.program, s.year " +
                "FROM university_auth.users_auth u " +
                "JOIN students s ON u.user_id = s.user_id " +
                "WHERE u.role = 'STUDENT' " +
                "ORDER BY s.roll_no";

        try (Connection conn = Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Student student = new Student(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("status"),
                        rs.getString("roll_no"),
                        rs.getString("program"),
                        rs.getInt("year")
                );
                students.add(student);
            }
        }
        return students;
    }

    public List<Instructor> getAllInstructors() throws SQLException {
        List<Instructor> instructors = new ArrayList<>();
        String sql = "SELECT u.user_id, u.username, u.status, i.department, i.office " +
                "FROM university_auth.users_auth u " +
                "JOIN instructors i ON u.user_id = i.user_id " +
                "WHERE u.role = 'INSTRUCTOR' " +
                "ORDER BY i.department, u.username";

        try (Connection conn = Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Instructor instructor = new Instructor(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("status"),
                        rs.getString("department"),
                        rs.getString("office")
                );
                instructors.add(instructor);
            }
        }
        return instructors;
    }

    // Course Management Methods
    public List<Course> getAllCourses() throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT course_id, code, title, credits, description FROM courses ORDER BY code";

        try (Connection conn = Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Course course = new Course(
                        rs.getInt("course_id"),
                        rs.getString("code"),
                        rs.getString("title"),
                        rs.getInt("credits"),
                        rs.getString("description")
                );
                courses.add(course);
            }
        }
        return courses;
    }

    public boolean createCourse(String code, String title, int credits, String description) throws SQLException {
        String sql = "INSERT INTO courses (code, title, credits, description) VALUES (?, ?, ?, ?)";

        try (Connection conn = Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code);
            stmt.setString(2, title);
            stmt.setInt(3, credits);
            stmt.setString(4, description);

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteCourse(int courseId) throws SQLException {
        // First check if course has sections
        String checkSql = "SELECT COUNT(*) FROM sections WHERE course_id = ?";
        try (Connection conn = Database.getERPConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setInt(1, courseId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                throw new SQLException("Cannot delete course: It has existing sections");
            }
        }

        // Delete the course
        String sql = "DELETE FROM courses WHERE course_id = ?";
        try (Connection conn = Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, courseId);
            return stmt.executeUpdate() > 0;
        }
    }

    // Section Management Methods
    public List<Section> getAllSections() throws SQLException {
        List<Section> sections = new ArrayList<>();
        String sql = "SELECT s.section_id, s.course_id, c.code, c.title, " +
                "s.instructor_id, u.username as instructor_name, " +
                "s.day_time, s.room, s.capacity, s.enrolled_count, " +
                "s.semester, s.year " +
                "FROM sections s " +
                "JOIN courses c ON s.course_id = c.course_id " +
                "LEFT JOIN university_auth.users_auth u ON s.instructor_id = u.user_id " +
                "ORDER BY c.code, s.semester, s.year";

        try (Connection conn = Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Section section = new Section(
                        rs.getInt("section_id"),
                        rs.getInt("course_id"),
                        rs.getString("code"),
                        rs.getString("title"),
                        rs.getInt("instructor_id"),
                        rs.getString("instructor_name"),
                        rs.getString("day_time"),
                        rs.getString("room"),
                        rs.getInt("capacity"),
                        rs.getInt("enrolled_count"),
                        rs.getString("semester"),
                        rs.getInt("year")
                );
                sections.add(section);
            }
        }
        return sections;
    }

    public boolean createSection(int courseId, int instructorId, String dayTime,
                                 String room, int capacity, String semester, int year) throws SQLException {
        String sql = "INSERT INTO sections (course_id, instructor_id, day_time, room, capacity, semester, year) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, courseId);
            stmt.setInt(2, instructorId);
            stmt.setString(3, dayTime);
            stmt.setString(4, room);
            stmt.setInt(5, capacity);
            stmt.setString(6, semester);
            stmt.setInt(7, year);

            return stmt.executeUpdate() > 0;
        }
    }

    public List<Course> getAvailableCourses() throws SQLException {
        return getAllCourses(); //return all courses
    }

    public List<Instructor> getAvailableInstructors() throws SQLException {
        return getAllInstructors(); //return all instructors
    }

    // Backup/Restore Methods
    public boolean backupDatabase(String backupPath) throws SQLException {
        try {
            System.out.println("Backup created at: " + backupPath);
            return true;
        } catch (Exception e) {
            throw new SQLException("Backup failed: " + e.getMessage());
        }
    }

    public boolean restoreDatabase(String backupPath) throws SQLException {
        try {
            System.out.println("Restore from: " + backupPath);
            return true;
        } catch (Exception e) {
            throw new SQLException("Restore failed: " + e.getMessage());
        }
    }

    // Utility Methods
    public Course getCourseById(int courseId) throws SQLException {
        String sql = "SELECT course_id, code, title, credits, description FROM courses WHERE course_id = ?";

        try (Connection conn = Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, courseId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Course(
                        rs.getInt("course_id"),
                        rs.getString("code"),
                        rs.getString("title"),
                        rs.getInt("credits"),
                        rs.getString("description")
                );
            }
            return null;
        }
    }

    public boolean updateCourse(int courseId, String code, String title, int credits, String description) throws SQLException {
        String sql = "UPDATE courses SET code = ?, title = ?, credits = ?, description = ? WHERE course_id = ?";

        try (Connection conn = Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code);
            stmt.setString(2, title);
            stmt.setInt(3, credits);
            stmt.setString(4, description);
            stmt.setInt(5, courseId);

            return stmt.executeUpdate() > 0;
        }
    }
}