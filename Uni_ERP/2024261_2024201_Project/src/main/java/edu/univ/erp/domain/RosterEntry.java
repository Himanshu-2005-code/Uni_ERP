package edu.univ.erp.domain;

/** One student row in an instructor's gradebook. */
public record RosterEntry(int enrollmentId, int studentId, String rollNo, String name, String program,
                          String finalGrade, Double finalPercentage) {
}
