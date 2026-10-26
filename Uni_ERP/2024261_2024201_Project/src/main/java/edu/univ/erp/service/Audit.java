package edu.univ.erp.service;

import edu.univ.erp.data.AuditDAO;
import edu.univ.erp.domain.User;

import java.sql.Connection;
import java.sql.SQLException;

/** Records who changed what, inside the same transaction as the change. */
final class Audit {

    private Audit() {
    }

    static void log(Connection c, String action, String details) throws SQLException {
        User u = Session.isSignedIn() ? Session.user() : null;
        AuditDAO.log(c, u == null ? null : u.id(), u == null ? "system" : u.username(), action, details);
    }
}
