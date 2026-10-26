package edu.univ.erp.data;

import edu.univ.erp.db.Sql;
import edu.univ.erp.domain.AuditEntry;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class AuditDAO {

    private AuditDAO() {
    }

    public static void log(Connection c, Integer userId, String username, String action, String details)
            throws SQLException {
        String d = details == null ? null : details.length() > 500 ? details.substring(0, 497) + "..." : details;
        Sql.update(c, "INSERT INTO audit_log (user_id, username, action, details) VALUES (?, ?, ?, ?)",
                userId, username, action, d);
    }

    public static List<AuditEntry> recent(Connection c, int limit) throws SQLException {
        return Sql.list(c, "SELECT log_id, user_id, username, action, details, created_at FROM audit_log "
                + "ORDER BY log_id DESC LIMIT ?", rs -> new AuditEntry(rs.getLong("log_id"),
                Sql.intOrNull(rs, "user_id"), rs.getString("username"), rs.getString("action"),
                rs.getString("details"), Sql.time(rs, "created_at")), limit);
    }
}
