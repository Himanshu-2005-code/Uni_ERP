package edu.univ.erp;

import edu.univ.erp.db.Database;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class TestConnection {
    public static void main(String[] args) {
        System.out.println("=== Testing Database Connection ===");

        try {
            // Test Auth DB connection
            System.out.println("1. Testing Auth Database...");
            try (Connection conn = Database.getAuthConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM users_auth")) {

                if (rs.next()) {
                    System.out.println("✓ Auth DB Connected! Users count: " + rs.getInt("count"));
                }
            }

            // Test ERP DB connection
            System.out.println("2. Testing ERP Database...");
            try (Connection conn = Database.getERPConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM courses")) {

                if (rs.next()) {
                    System.out.println("✓ ERP DB Connected! Courses count: " + rs.getInt("count"));
                }
            }

            // Test if users exist
            System.out.println("3. Testing Users...");
            try (Connection conn = Database.getAuthConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT username, role, password_hash FROM users_auth")) {

                System.out.println("Available users:");
                while (rs.next()) {
                    System.out.println("  - " + rs.getString("username") + " (" + rs.getString("role") + ")");
                    System.out.println("    Hash: " + rs.getString("password_hash"));
                }
            }

        } catch (Exception e) {
            System.err.println("✗ Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}