package edu.univ.erp.domain;

import java.time.LocalDateTime;

/** A login account. */
public record User(int id, String username, Role role, String status, String fullName, String email,
                   LocalDateTime lastLogin, LocalDateTime createdAt, int failedAttempts) {

    public String displayName() {
        return fullName == null || fullName.isBlank() ? username : fullName;
    }

    public boolean isLocked() {
        return "LOCKED".equals(status);
    }

    public String initials() {
        String[] parts = displayName().trim().split("\\s+");
        String a = parts[0].substring(0, 1);
        String b = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (a + b).toUpperCase();
    }
}
