package edu.univ.erp.service;

import edu.univ.erp.data.InstructorDAO;
import edu.univ.erp.data.GradeDAO;
import edu.univ.erp.data.StudentDAO;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.domain.Student;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class InstructorService {
    private InstructorDAO instructorDAO;
    private GradeDAO gradeDAO;
    private StudentDAO studentDAO;

    public InstructorService() {
        this.instructorDAO = new InstructorDAO();
        this.gradeDAO = new GradeDAO();
        this.studentDAO = new StudentDAO();
    }

    public List<Section> getInstructorSections(int instructorId) {
        try {
            List<Section> sections = instructorDAO.getInstructorSections(instructorId);

            // Update enrollment counts for all sections
            for (Section section : sections) {
                gradeDAO.updateEnrollmentCount(section.getSectionId());
            }

            return sections;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get instructor sections: " + e.getMessage());
        }
    }

    public List<Enrollment> getSectionEnrollments(int sectionId) {
        try {
            return gradeDAO.getSectionEnrollments(sectionId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get section enrollments: " + e.getMessage());
        }
    }

    public boolean saveGrades(int sectionId, Map<Integer, Map<String, Double>> grades) {
        try {
            return gradeDAO.saveGrades(sectionId, grades);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save grades: " + e.getMessage());
        }
    }

    public Map<String, Object> computeSectionStats(int sectionId) {
        try {
            return gradeDAO.computeSectionStats(sectionId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to compute section stats: " + e.getMessage());
        }
    }

    public boolean exportGradesToCsv(int sectionId, String filename) {
        try {
            List<Enrollment> enrollments = getSectionEnrollments(sectionId);

            //CSV implementation
            try (java.io.FileWriter writer = new java.io.FileWriter(filename)) {
                writer.write("Enrollment ID,Student ID,Quiz Score,Midterm Score,Final Score,Final Percentage,Final Grade\n");

                for (Enrollment enrollment : enrollments) {
                    List<Grade> grades = gradeDAO.getStudentGrades(enrollment.getEnrollmentId());

                    double quizScore = getComponentScore(grades, "quiz");
                    double midtermScore = getComponentScore(grades, "midterm");
                    double finalScore = getComponentScore(grades, "final");

                    writer.write(String.format("%d,%d,%.2f,%.2f,%.2f,%.1f%%,%s\n",
                            enrollment.getEnrollmentId(),
                            enrollment.getStudentId(),
                            quizScore,
                            midtermScore,
                            finalScore,
                            enrollment.getFinalPercentage(),
                            enrollment.getFinalGrade() != null ? enrollment.getFinalGrade() : "N/A"
                    ));
                }
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to export grades: " + e.getMessage());
        }
    }

    private double getComponentScore(List<Grade> grades, String component) {
        if (grades == null) return 0.0;
        for (Grade grade : grades) {
            if (grade.getComponent().equalsIgnoreCase(component)) {
                return grade.getScore();
            }
        }
        return 0.0;
    }

    public Student getStudentDetails(int studentId) {
        try {
            return studentDAO.getStudentProfile(studentId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get student details: " + e.getMessage());
        }
    }

    public Map<Integer, Map<String, Double>> getCurrentGrades(int sectionId) {
        try {
            List<Enrollment> enrollments = getSectionEnrollments(sectionId);
            Map<Integer, Map<String, Double>> currentGrades = new HashMap<>();

            for (Enrollment enrollment : enrollments) {
                List<Grade> grades = gradeDAO.getStudentGrades(enrollment.getEnrollmentId());
                Map<String, Double> studentGrades = new HashMap<>();

                for (Grade grade : grades) {
                    studentGrades.put(grade.getComponent(), grade.getScore());
                }

                currentGrades.put(enrollment.getEnrollmentId(), studentGrades);
            }

            return currentGrades;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get current grades: " + e.getMessage());
        }
    }

    public boolean updateEnrollmentCount(int sectionId) {
        try {
            return gradeDAO.updateEnrollmentCount(sectionId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update enrollment count: " + e.getMessage());
        }
    }
}