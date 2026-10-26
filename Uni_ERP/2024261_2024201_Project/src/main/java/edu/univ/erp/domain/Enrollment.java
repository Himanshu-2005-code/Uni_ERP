package edu.univ.erp.domain;

import java.time.LocalDateTime;

/** A student's registration in a section, with the section details attached. */
public record Enrollment(int id, int studentId, int sectionId, String status, LocalDateTime enrolledAt,
                         String finalGrade, Double finalPercentage, Section section) {

    public boolean isActive() {
        return "REGISTERED".equals(status);
    }

    public boolean hasFinal() {
        return finalGrade != null && !finalGrade.isBlank();
    }
}
