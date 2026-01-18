package edu.univ.erp.data;

import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.db.Database;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InstructorDAO {

    public List<Section> getInstructorSections(int instructorId) throws SQLException {
        List<Section> sections = new ArrayList<>();
        String sql = "SELECT s.section_id, s.course_id, c.code, c.title, c.credits, " +
                "s.instructor_id, u.username as instructor_name, " +
                "s.day_time, s.room, s.capacity, s.enrolled_count, " +
                "s.semester, s.year " +
                "FROM sections s " +
                "JOIN courses c ON s.course_id = c.course_id " +
                "LEFT JOIN university_auth.users_auth u ON s.instructor_id = u.user_id " +
                "WHERE s.instructor_id = ? " +
                "ORDER BY s.semester DESC, s.year DESC, c.code";

        try (Connection conn = Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, instructorId);
            ResultSet rs = stmt.executeQuery();

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

    public boolean addInstructor(Instructor instructor) throws SQLException {
        String sql = "INSERT INTO instructors (user_id, department, office) VALUES (?, ?, ?)";

        try (Connection conn = Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, instructor.getUserId());
            stmt.setString(2, instructor.getDepartment());
            stmt.setString(3, instructor.getOffice());

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateInstructor(Instructor instructor) throws SQLException {
        String sql = "UPDATE instructors SET department = ?, office = ? WHERE user_id = ?";

        try (Connection conn = Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, instructor.getDepartment());
            stmt.setString(2, instructor.getOffice());
            stmt.setInt(3, instructor.getUserId());

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteInstructor(int userId) throws SQLException {
        String sql = "DELETE FROM instructors WHERE user_id = ?";

        try (Connection conn = Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        }
    }
}