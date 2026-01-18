package edu.univ.erp.domain;

public class Grade {
    private int gradeId;
    private int enrollmentId;
    private String component;
    private double score;
    private double maxScore;
    private double weight;

    public Grade(int gradeId, int enrollmentId, String component, double score, double maxScore, double weight) {
        this.gradeId = gradeId;
        this.enrollmentId = enrollmentId;
        this.component = component;
        this.score = score;
        this.maxScore = maxScore;
        this.weight = weight;
    }

    // Getters and setters
    public int getGradeId() { return gradeId; }
    public int getEnrollmentId() { return enrollmentId; }
    public String getComponent() { return component; }
    public double getScore() { return score; }
    public double getMaxScore() { return maxScore; }
    public double getWeight() { return weight; }

    public double getPercentage() {
        return (score / maxScore) * 100;
    }

    public double getWeightedScore() {
        return (score / maxScore) * weight;
    }
}