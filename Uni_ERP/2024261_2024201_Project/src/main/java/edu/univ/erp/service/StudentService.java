package edu.univ.erp.service;

import edu.univ.erp.data.StudentDAO;
import edu.univ.erp.domain.Student;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Grade;
import java.sql.SQLException;
import java.util.List;

public class StudentService {
    private StudentDAO studentDAO;

    public StudentService() {
        this.studentDAO = new StudentDAO();
    }

    public Student getStudentProfile(int userId) {
        try {
            return studentDAO.getStudentProfile(userId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get student profile: " + e.getMessage());
        }
    }

    public List<Section> getAvailableSections() {
        try {
            return studentDAO.getAvailableSections();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get available sections: " + e.getMessage());
        }
    }

    public List<Enrollment> getStudentEnrollments(int studentId) {
        try {
            return studentDAO.getStudentEnrollments(studentId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get enrollments: " + e.getMessage());
        }
    }

    public boolean enrollInSection(int studentId, int sectionId) {
        try {
            return studentDAO.enrollInSection(studentId, sectionId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to enroll in section: " + e.getMessage());
        }
    }

    public boolean dropSection(int enrollmentId) {
        try {
            return studentDAO.dropSection(enrollmentId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to drop section: " + e.getMessage());
        }
    }

    public List<Grade> getStudentGrades(int studentId) {
        try {
            return studentDAO.getStudentGrades(studentId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get grades: " + e.getMessage());
        }
    }
}