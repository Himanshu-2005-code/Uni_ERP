package edu.univ.erp.domain;

/** An assessment in a section, e.g. "Midterm" out of 30 marks worth 30% of the course. */
public record GradeComponent(int id, int sectionId, String name, double maxScore, double weight, int position) {
}
