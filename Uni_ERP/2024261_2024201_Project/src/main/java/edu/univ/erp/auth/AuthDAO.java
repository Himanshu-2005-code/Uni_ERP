package edu.univ.erp.auth;

import edu.univ.erp.domain.User;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;

public class AuthDAO {

    public User authenticate(String username, String password) throws SQLException {

        //Hashing
        String sql = "SELECT user_id, username, role, password_hash, status, failed_attempts " +
                    "FROM users_auth WHERE username = ?";


        try (Connection conn = edu.univ.erp.db.Database.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedHash = rs.getString("password_hash");

                if ("LOCKED".equals(rs.getString("status"))) {
                    throw new RuntimeException("Account is locked. Contact administrator.");
                }


                boolean passwordValid = false;
                try {
                    passwordValid = BCrypt.checkpw(password, storedHash);
                } catch (Exception e) {
                    System.out.println("BCrypt verification failed: " + e.getMessage());
                }

                if (passwordValid) {
                    System.out.println("Password validation SUCCESSFUL!");
                    updateLastLogin(rs.getInt("user_id"));
                    resetFailedAttempts(rs.getInt("user_id"));

                    return new User(
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("status")
                    );
                } else {
                    System.out.println("Password validation FAILED!");
                    incrementFailedAttempts(rs.getInt("user_id"));
                    throw new RuntimeException("Invalid username or password");
                }
            } else {
                System.out.println("No user found with username: " + username);
                throw new RuntimeException("Invalid username or password");
            }
        }
    }


    public boolean createUser(String username, String password, String role) throws SQLException {
        String sql = "INSERT INTO users_auth (username, role, password_hash) VALUES (?, ?, ?)";

        try (Connection conn = edu.univ.erp.db.Database.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, role);

            stmt.setString(3, BCrypt.hashpw(password, BCrypt.gensalt()));

            return stmt.executeUpdate() > 0;
        }
    }

    private void updateLastLogin(int userId) throws SQLException {
        String sql = "UPDATE users_auth SET last_login = NOW() WHERE user_id = ?";
        try (Connection conn = edu.univ.erp.db.Database.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    private void incrementFailedAttempts(int userId) throws SQLException {
        String sql = "UPDATE users_auth SET failed_attempts = failed_attempts + 1 WHERE user_id = ?";
        try (Connection conn = edu.univ.erp.db.Database.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    private void resetFailedAttempts(int userId) throws SQLException {
        String sql = "UPDATE users_auth SET failed_attempts = 0 WHERE user_id = ?";
        try (Connection conn = edu.univ.erp.db.Database.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }
}