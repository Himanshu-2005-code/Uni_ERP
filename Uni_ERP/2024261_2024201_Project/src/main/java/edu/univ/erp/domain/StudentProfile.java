package edu.univ.erp.domain;

public record StudentProfile(int userId, String username, String fullName, String email, String status,
                             String rollNo, String program, int year) {

    public String displayName() {
        return fullName == null || fullName.isBlank() ? username : fullName;
    }
}
