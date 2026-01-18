package edu.univ.erp;

import edu.univ.erp.ui.LoginFrame;
import edu.univ.erp.db.Database;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;


public class Main {
    public static void main(String[] args) {
        // Set modern look and feeL using FlatLightLaf
    
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

        // Test database connections first
        System.out.println("=== Starting University ERP ===");
        Database.testConnections();

        // Start with login screen
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });

    }
}