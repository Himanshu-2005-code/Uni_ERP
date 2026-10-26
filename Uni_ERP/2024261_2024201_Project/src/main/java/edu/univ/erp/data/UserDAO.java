package edu.univ.erp.data;

import edu.univ.erp.db.Sql;
import edu.univ.erp.domain.Role;
import edu.univ.erp.domain.User;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Login accounts in university_auth.users_auth. */
public final class UserDAO {

    public record Credentials(User user, String passwordHash) {
    }

    private static final String COLS = "user_id, username, role, status, full_name, email, last_login, "
            + "created_at, failed_attempts";
    private static final String TABLE = "university_auth.users_auth";

    private UserDAO() {
    }

    static User map(ResultSet rs) throws SQLException {
        return new User(rs.getInt("user_id"), rs.getString("username"), Role.valueOf(rs.getString("role")),
                rs.getString("status"), rs.getString("full_name"), rs.getString("email"),
                Sql.time(rs, "last_login"), Sql.time(rs, "created_at"), rs.getInt("failed_attempts"));
    }

    public static Optional<Credentials> credentials(Connection c, String username) throws SQLException {
        return Sql.one(c, "SELECT " + COLS + ", password_hash FROM " + TABLE + " WHERE username = ?",
                rs -> new Credentials(map(rs), rs.getString("password_hash")), username);
    }

    public static Optional<String> passwordHash(Connection c, int id) throws SQLException {
        return Sql.one(c, "SELECT password_hash FROM " + TABLE + " WHERE user_id = ?", rs -> rs.getString(1), id);
    }

    public static Optional<User> find(Connection c, int id) throws SQLException {
        return Sql.one(c, "SELECT " + COLS + " FROM " + TABLE + " WHERE user_id = ?", UserDAO::map, id);
    }

    public static List<User> listAll(Connection c) throws SQLException {
        return Sql.list(c, "SELECT " + COLS + " FROM " + TABLE + " ORDER BY role, username", UserDAO::map);
    }

    public static boolean usernameExists(Connection c, String username) throws SQLException {
        return Sql.count(c, "SELECT COUNT(*) FROM " + TABLE + " WHERE username = ?", username) > 0;
    }

    public static int insert(Connection c, String username, Role role, String hash, String fullName, String email)
            throws SQLException {
        return Sql.insert(c, "INSERT INTO " + TABLE + " (username, role, password_hash, full_name, email) "
                + "VALUES (?, ?, ?, ?, ?)", username, role, hash, fullName, email);
    }

    public static void updateContact(Connection c, int id, String fullName, String email) throws SQLException {
        Sql.update(c, "UPDATE " + TABLE + " SET full_name = ?, email = ? WHERE user_id = ?", fullName, email, id);
    }

    public static void setStatus(Connection c, int id, String status) throws SQLException {
        Sql.update(c, "UPDATE " + TABLE + " SET status = ?, failed_attempts = 0 WHERE user_id = ?", status, id);
    }

    public static void setPassword(Connection c, int id, String hash) throws SQLException {
        Sql.update(c, "UPDATE " + TABLE + " SET password_hash = ? WHERE user_id = ?", hash, id);
    }

    /** Adds a failed attempt and locks the account once the threshold is reached. Returns the new count. */
    public static int recordFailure(Connection c, int id, int threshold) throws SQLException {
        // MySQL applies SET clauses left to right, so status must be computed before the counter changes
        Sql.update(c, "UPDATE " + TABLE + " SET status = IF(failed_attempts + 1 >= ?, 'LOCKED', status), "
                + "failed_attempts = failed_attempts + 1 WHERE user_id = ?", threshold, id);
        return Sql.count(c, "SELECT failed_attempts FROM " + TABLE + " WHERE user_id = ?", id);
    }

    public static void recordSuccess(Connection c, int id) throws SQLException {
        Sql.update(c, "UPDATE " + TABLE + " SET failed_attempts = 0, last_login = NOW() WHERE user_id = ?", id);
    }

    public static void delete(Connection c, int id) throws SQLException {
        Sql.update(c, "DELETE FROM " + TABLE + " WHERE user_id = ?", id);
    }

    public static Map<String, Integer> countByRole(Connection c) throws SQLException {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (Role r : Role.values()) m.put(r.name(), 0);
        Sql.list(c, "SELECT role, COUNT(*) n FROM " + TABLE + " GROUP BY role",
                rs -> m.put(rs.getString("role"), rs.getInt("n")));
        return m;
    }
}
