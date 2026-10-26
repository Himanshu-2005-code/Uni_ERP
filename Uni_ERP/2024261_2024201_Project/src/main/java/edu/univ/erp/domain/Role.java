package edu.univ.erp.domain;

public enum Role {
    STUDENT("Student"),
    INSTRUCTOR("Instructor"),
    ADMIN("Administrator");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
