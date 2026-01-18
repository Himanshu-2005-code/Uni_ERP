package edu.univ.erp.data;

import edu.univ.erp.domain.Grade;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.db.Database;
import java.sql.*;
import java.util.*;

public class GradeDAO {

    public List<Enrollment> getSectionEnrollments(int sectionId) throws SQLException {
        List<Enrollment> enrollments = new ArrayList<>();
        String sql = "SELECT e.enrollment_id, e.student_id, e.section_id, e.status, " +
                "e.enrollment_date, e.final_grade, " +
                "u.username, s.roll_no, s.program " +
                "FROM enrollments e " +
                "JOIN university_auth.users_auth u ON e.student_id = u.user_id " +
                "JOIN students s ON e.student_id = s.user_id " +
                "WHERE e.section_id = ? AND e.status = 'REGISTERED' " +
                "ORDER BY s.roll_no";

        try (Connection conn = Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, sectionId);
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
                //We have not set final_percentage since it is not in the table
                enrollments.add(enrollment);
            }
        }
        return enrollments;
    }


    public List<Grade> getStudentGrades(int enrollmentId) throws SQLException {
        List<Grade> grades = new ArrayList<>();
        String sql = "SELECT grade_id, enrollment_id, component, score, max_score, weight " +
                "FROM grades WHERE enrollment_id = ? ORDER BY component";

        try (Connection conn = Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, enrollmentId);
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

    public boolean saveGrades(int sectionId, Map<Integer, Map<String, Double>> grades) throws SQLException {
        String deleteSql = "DELETE FROM grades WHERE enrollment_id IN " +
                "(SELECT enrollment_id FROM enrollments WHERE section_id = ?)";
        String insertSql = "INSERT INTO grades (enrollment_id, component, score, max_score, weight) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = Database.getERPConnection()) {
            conn.setAutoCommit(false);

            // Delete existing grades for this section
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setInt(1, sectionId);
                deleteStmt.executeUpdate();
            }

            // Insert new grades
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                for (Map.Entry<Integer, Map<String, Double>> entry : grades.entrySet()) {
                    int enrollmentId = entry.getKey();
                    Map<String, Double> studentGrades = entry.getValue();

                    for (Map.Entry<String, Double> gradeEntry : studentGrades.entrySet()) {
                        String component = gradeEntry.getKey();
                        Double score = gradeEntry.getValue();

                        if (score != null && score >= 0) {
                            double maxScore = getMaxScoreForComponent(component);
                            double weight = getWeightForComponent(component);

                            // Validate score doesn't exceed max score
                            double actualScore = Math.min(score, maxScore);

                            insertStmt.setInt(1, enrollmentId);
                            insertStmt.setString(2, component);
                            insertStmt.setDouble(3, actualScore);
                            insertStmt.setDouble(4, maxScore);
                            insertStmt.setDouble(5, weight);
                            insertStmt.addBatch();
                        }
                    }
                }
                insertStmt.executeBatch();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            throw e;
        }
    }

    public Map<String, Object> computeSectionStats(int sectionId) throws SQLException {
        Map<String, Object> stats = new HashMap<>();
        String sql = "SELECT component, " +
                "AVG(score) as avg_score, " +
                "MAX(score) as max_score, " +
                "MIN(score) as min_score, " +
                "COUNT(*) as count, " +
                "STDDEV(score) as std_dev " +
                "FROM grades g " +
                "JOIN enrollments e ON g.enrollment_id = e.enrollment_id " +
                "WHERE e.section_id = ? " +
                "GROUP BY component";

        try (Connection conn = Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, sectionId);
            ResultSet rs = stmt.executeQuery();

            List<Map<String, Object>> componentStats = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> component = new HashMap<>();
                component.put("component", rs.getString("component"));
                component.put("average", rs.getDouble("avg_score"));
                component.put("maximum", rs.getDouble("max_score"));
                component.put("minimum", rs.getDouble("min_score"));
                component.put("count", rs.getInt("count"));
                component.put("stdDev", rs.getDouble("std_dev"));
                componentStats.add(component);
            }

            stats.put("componentStats", componentStats);
            stats.put("totalStudents", getTotalStudentsInSection(sectionId));
            stats.put("studentsWithGrades", getStudentsWithGradesCount(sectionId));

            // Add capacity information
            Map<String, Object> capacityInfo = getSectionCapacityInfo(sectionId);
            stats.putAll(capacityInfo);
        }
        return stats;
    }

    private int getTotalStudentsInSection(int sectionId) throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM enrollments WHERE section_id = ? AND status = 'REGISTERED'";

        try (Connection conn = Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, sectionId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }
            return 0;
        }
    }

    private int getStudentsWithGradesCount(int sectionId) throws SQLException {
        String sql = "SELECT COUNT(DISTINCT e.enrollment_id) as count " +
                "FROM enrollments e " +
                "JOIN grades g ON e.enrollment_id = g.enrollment_id " +
                "WHERE e.section_id = ? AND e.status = 'REGISTERED'";

        try (Connection conn = Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, sectionId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }
            return 0;
        }
    }

    private Map<String, Object> getSectionCapacityInfo(int sectionId) throws SQLException {
        Map<String, Object> capacityInfo = new HashMap<>();
        String sql = "SELECT capacity, enrolled_count FROM sections WHERE section_id = ?";

        try (Connection conn = Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, sectionId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                capacityInfo.put("capacity", rs.getInt("capacity"));
                capacityInfo.put("enrolledCount", rs.getInt("enrolled_count"));
                capacityInfo.put("availableSeats", rs.getInt("capacity") - rs.getInt("enrolled_count"));
            }
        }
        return capacityInfo;
    }

    // Update enrollment count when students enroll/drop
    public boolean updateEnrollmentCount(int sectionId) throws SQLException {
        String sql = "UPDATE sections SET enrolled_count = " +
                "(SELECT COUNT(*) FROM enrollments WHERE section_id = ? AND status = 'REGISTERED') " +
                "WHERE section_id = ?";

        try (Connection conn = Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, sectionId);
            stmt.setInt(2, sectionId);
            return stmt.executeUpdate() > 0;
        }
    }


    private double getMaxScoreForComponent(String component) {
        switch (component.toLowerCase()) {
            case "quiz": return 20.0;
            case "midterm": return 30.0;
            case "final": return 50.0;
            default: return 100.0;
        }
    }

    private double getWeightForComponent(String component) {
        switch (component.toLowerCase()) {
            case "quiz": return 0.2;
            case "midterm": return 0.3;
            case "final": return 0.5;
            default: return 1.0;
        }
    }
}