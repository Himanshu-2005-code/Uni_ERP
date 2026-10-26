package edu.univ.erp.domain;

import java.time.LocalDateTime;

/** A notice from an admin (whole campus, sectionId null) or an instructor (one section). */
public record Announcement(int id, int authorId, String authorName, Integer sectionId, String sectionLabel,
                           String title, String body, LocalDateTime createdAt) {

    public boolean isCampusWide() {
        return sectionId == null;
    }
}
