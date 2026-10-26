package edu.univ.erp.domain;

/** A recorded score for one component of one enrollment. */
public record GradeEntry(int enrollmentId, String component, double score, double maxScore, double weight) {

    public double percent() {
        return maxScore <= 0 ? 0 : score / maxScore * 100.0;
    }
}
