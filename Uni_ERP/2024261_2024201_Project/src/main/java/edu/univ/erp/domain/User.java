package edu.univ.erp.domain;

public class User {
    protected int userId;
    protected String username;
    protected String role;
    protected String status;

    public User(int userId, String username, String role, String status) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.status = status;
    }

    // Getters and setters
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getStatus() { return status; }

    public void setUserId(int userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setRole(String role) { this.role = role; }
    public void setStatus(String status) { this.status = status; }
}