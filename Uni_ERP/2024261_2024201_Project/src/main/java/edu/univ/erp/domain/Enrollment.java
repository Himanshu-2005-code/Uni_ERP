package edu.univ.erp.domain;

import java.time.LocalDateTime;

public class Enrollment {
    private int enrollmentId;
    private int studentId;
    private int sectionId;
    private String status;
    private LocalDateTime enrollmentDate;
    private String finalGrade;
    private double finalPercentage; // Added final percentage
    private Section section;

    public Enrollment(int enrollmentId, int studentId, int sectionId, String status,
                      LocalDateTime enrollmentDate, String finalGrade) {
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.sectionId = sectionId;
        this.status = status;
        this.enrollmentDate = enrollmentDate;
        this.finalGrade = finalGrade;
        this.finalPercentage = 0.0;
    }

    // Getters and setters
    public int getEnrollmentId() { return enrollmentId; }
    public int getStudentId() { return studentId; }
    public int getSectionId() { return sectionId; }
    public String getStatus() { return status; }
    public LocalDateTime getEnrollmentDate() { return enrollmentDate; }
    public String getFinalGrade() { return finalGrade; }
    public double getFinalPercentage() { return finalPercentage; }
    public Section getSection() { return section; }

    public void setSection(Section section) { this.section = section; }
    public void setFinalGrade(String finalGrade) { this.finalGrade = finalGrade; }
    public void setFinalPercentage(double finalPercentage) { this.finalPercentage = finalPercentage; }

    public boolean isActive() {
        return "REGISTERED".equals(status);
    }

    public String getFinalPercentageFormatted() {
        return String.format("%.1f%%", finalPercentage);
    }
}