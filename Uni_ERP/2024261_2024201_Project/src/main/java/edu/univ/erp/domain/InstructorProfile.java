package edu.univ.erp.domain;

public record InstructorProfile(int userId, String username, String fullName, String email, String status,
                                String department, String office) {

    public String displayName() {
        return fullName == null || fullName.isBlank() ? username : fullName;
    }

    @Override
    public String toString() {
        return displayName() + (department == null ? "" : " · " + department);
    }
}
