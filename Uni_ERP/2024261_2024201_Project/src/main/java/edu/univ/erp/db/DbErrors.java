package edu.univ.erp.db;

import edu.univ.erp.service.ServiceException;

import java.sql.SQLException;

/** Turns raw SQL errors into messages a user can act on. */
final class DbErrors {

    private DbErrors() {
    }

    static RuntimeException wrap(SQLException e) {
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        int code = e.getErrorCode();
        if (code == 1062 || state.equals("23505")) {
            return new ServiceException("That record already exists (duplicate value).", e);
        }
        if (code == 1451 || code == 1452 || state.startsWith("23")) {
            return new ServiceException("This record is linked to other data and can't be changed that way.", e);
        }
        if (state.startsWith("08") || code == 0 && e.getMessage() != null && e.getMessage().contains("Communications")) {
            return new ServiceException("Lost connection to the database. Check that the server is running.", e);
        }
        return new ServiceException("Database error: " + e.getMessage(), e);
    }
}
