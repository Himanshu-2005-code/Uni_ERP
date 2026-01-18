package edu.univ.erp.service;

import java.sql.*;

public class MaintenanceService {

    public boolean toggleMaintenanceMode(boolean enable) {
        String sql = "UPDATE settings SET setting_value = ? WHERE setting_key = 'maintenance_mode'";

        try (Connection conn = edu.univ.erp.db.Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, Boolean.toString(enable));
            boolean success = stmt.executeUpdate() > 0;

            if (success) {
                System.out.println("Maintenance mode " + (enable ? "ENABLED" : "DISABLED"));
            }

            return success;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to toggle maintenance mode: " + e.getMessage());
        }
    }

    public boolean isMaintenanceMode() {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = 'maintenance_mode'";

        try (Connection conn = edu.univ.erp.db.Database.getERPConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return Boolean.parseBoolean(rs.getString("setting_value"));
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check maintenance mode: " + e.getMessage());
        }
    }

    public boolean canUserModifyData(String userRole) {
        if (isMaintenanceMode()) {
            return "ADMIN".equals(userRole);
        }
        return true;
    }
}