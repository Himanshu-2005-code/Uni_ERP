package edu.univ.erp.data;

import edu.univ.erp.domain.Student;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Grade;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {

    public Student getStudentProfile(int userId) throws SQLException {
        String sql = "SELECT s.user_id, u.username, u.status, s.roll_no, s.program, s.year FROM students s JOIN university_auth.users_auth u ON s.user_id = u.user_id WHERE s.user_id = ?";

        try (Connection conn = edu.univ.erp.db.Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Student(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("status"),
                        rs.getString("roll_no"),
                        rs.getString("program"),
                        rs.getInt("year")
                );
            }
            return null;
        }
    }

    public boolean addStudent(Student student) {
        String sql = "INSERT INTO students (user_id, roll_no, program, year) VALUES (?, ?, ?, ?)";

        try (Connection conn = edu.univ.erp.db.Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, student.getUserId());
            stmt.setString(2, student.getRollNo());
            stmt.setString(3, student.getProgram());
            stmt.setInt(4, student.getYear());

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Error adding student: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public List<Section> getAvailableSections() throws SQLException {
        List<Section> sections = new ArrayList<>();
        String sql = "SELECT s.section_id, s.course_id, c.code, c.title, s.instructor_id, u.username as instructor_name, s.day_time, s.room, s.capacity, s.enrolled_count, s.semester, s.year FROM sections s JOIN courses c ON s.course_id = c.course_id LEFT JOIN university_auth.users_auth u ON s.instructor_id = u.user_id WHERE s.semester = (SELECT setting_value FROM settings WHERE setting_key = 'current_semester') AND s.year = (SELECT setting_value FROM settings WHERE setting_key = 'current_year') ORDER BY c.code, s.day_time";

        try (Connection conn = edu.univ.erp.db.Database.getERPConnection();
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

    public List<Enrollment> getStudentEnrollments(int studentId) throws SQLException {
        List<Enrollment> enrollments = new ArrayList<>();
        String sql = "SELECT e.enrollment_id, e.student_id, e.section_id, e.status, e.enrollment_date, e.final_grade, s.section_id, s.course_id, c.code, c.title, s.instructor_id, u.username as instructor_name, s.day_time, s.room, s.capacity, s.enrolled_count, s.semester, s.year FROM enrollments e JOIN sections s ON e.section_id = s.section_id JOIN courses c ON s.course_id = c.course_id LEFT JOIN university_auth.users_auth u ON s.instructor_id = u.user_id WHERE e.student_id = ? AND e.status = 'REGISTERED' ORDER BY s.day_time";

        try (Connection conn = edu.univ.erp.db.Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Enrollment enrollment = new Enrollment(
                        rs.getInt("enrollment_id"),
                        rs.getInt("student_id"),
                        rs.getInt("section_id"),
                        rs.getString("status"),
                        rs.getTimestamp("enrollment_date").toLocalDateTime(),
                        rs.getString("final_grade")
                );

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

                enrollment.setSection(section);
                enrollments.add(enrollment);
            }
        }
        return enrollments;
    }

    public boolean enrollInSection(int studentId, int sectionId) throws SQLException {

        try {
            if (isAlreadyEnrolled(studentId, sectionId)) {
                throw new RuntimeException("Already enrolled in this section");
            }

            if (!hasAvailableSeats(sectionId)) {
                throw new RuntimeException("Section is full");
            }

            String sql = "INSERT INTO enrollments (student_id, section_id, status, enrollment_date) VALUES (?, ?, 'REGISTERED', NOW())";

            try (Connection conn = edu.univ.erp.db.Database.getERPConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, studentId);
                stmt.setInt(2, sectionId);
                int affected = stmt.executeUpdate();

                if (affected > 0) {
                    updateEnrolledCount(sectionId, 1);
                    return true;
                }
                return false;
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception in enrollment: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            System.err.println("General Exception in enrollment: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public boolean dropSection(int enrollmentId) throws SQLException {
        String sql = "UPDATE enrollments SET status = 'DROPPED' WHERE enrollment_id = ?";

        try (Connection conn = edu.univ.erp.db.Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, enrollmentId);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                int sectionId = getSectionIdByEnrollment(enrollmentId);
                updateEnrolledCount(sectionId, -1);
                return true;
            }
            return false;
        }
    }

    private boolean isAlreadyEnrolled(int studentId, int sectionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM enrollments WHERE student_id = ? AND section_id = ? AND status = 'REGISTERED'";

        try (Connection conn = edu.univ.erp.db.Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            stmt.setInt(2, sectionId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        }
    }

    private boolean hasAvailableSeats(int sectionId) throws SQLException {
        String sql = "SELECT capacity, enrolled_count FROM sections WHERE section_id = ?";

        try (Connection conn = edu.univ.erp.db.Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, sectionId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("enrolled_count") < rs.getInt("capacity");
            }
            return false;
        }
    }

    private void updateEnrolledCount(int sectionId, int change) throws SQLException {
        String sql = "UPDATE sections SET enrolled_count = enrolled_count + ? WHERE section_id = ?";

        try (Connection conn = edu.univ.erp.db.Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, change);
            stmt.setInt(2, sectionId);
            stmt.executeUpdate();
        }
    }

    private int getSectionIdByEnrollment(int enrollmentId) throws SQLException {
        String sql = "SELECT section_id FROM enrollments WHERE enrollment_id = ?";

        try (Connection conn = edu.univ.erp.db.Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, enrollmentId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("section_id");
            }
            return -1;
        }
    }

    public List<Grade> getStudentGrades(int studentId) throws SQLException {
        List<Grade> grades = new ArrayList<>();
        String sql = "SELECT g.grade_id, g.enrollment_id, g.component, g.score, g.max_score, g.weight, c.code, c.title, s.section_id FROM grades g JOIN enrollments e ON g.enrollment_id = e.enrollment_id JOIN sections s ON e.section_id = s.section_id JOIN courses c ON s.course_id = c.course_id WHERE e.student_id = ? ORDER BY c.code, g.component";

        try (Connection conn = edu.univ.erp.db.Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Grade grade = new Grade(
                        rs.getInt("grade_id"),
                        rs.getInt("enrollment_id"),
                        rs.getString("component"),
                        rs.getDouble("score"),
                        rs.getDouble("max_score"),
                        rs.getDouble("weight")
                );
                grades.add(grade);
            }
        }
        return grades;
    }
}