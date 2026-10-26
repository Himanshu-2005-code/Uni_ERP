package edu.univ.erp.domain;

import java.time.LocalDateTime;

public record AuditEntry(long id, Integer userId, String username, String action, String details,
                         LocalDateTime createdAt) {
}
