package edu.univ.erp.service;

import edu.univ.erp.domain.User;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import edu.univ.erp.auth.AuthDAO;

public class AuthService {
    private AuthDAO authDAO;

    public AuthService() {
        this.authDAO = new AuthDAO();
    }

    
    //Authenticate a user with username and password
    public User login(String username, String password) {
        try {
            return authDAO.authenticate(username, password);
        } catch (SQLException e) {
            throw new RuntimeException("Database error during authentication: " + e.getMessage());
        } catch (RuntimeException e) {
            throw e;
        }
    }

    
    //Create a new user in the authentication system
    public boolean createUser(String username, String password, String role) {
        try {
            return authDAO.createUser(username, password, role);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create user: " + e.getMessage());
        }
    }

    
    //Get user ID by username
    public int getUserIdByUsername(String username) {
        String sql = "SELECT user_id FROM users_auth WHERE username = ?";

        try (Connection conn = edu.univ.erp.db.Database.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("user_id");
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get user ID: " + e.getMessage());
        }
    }

    
    //Check if a username already exists
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users_auth WHERE username = ?";

        try (Connection conn = edu.univ.erp.db.Database.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check username: " + e.getMessage());
        }
    }
    
    //Delete a user by user ID
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users_auth WHERE user_id = ?";

        try (Connection conn = edu.univ.erp.db.Database.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete user: " + e.getMessage());
        }
    }

    
    //Change user password
    public boolean changePassword(int userId, String newPassword) {
        String sql = "UPDATE users_auth SET password_hash = ? WHERE user_id = ?";

        try (Connection conn = edu.univ.erp.db.Database.getAuthConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            
            stmt.setString(1, newPassword);
            stmt.setInt(2, userId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to change password: " + e.getMessage());
        }
    }
}