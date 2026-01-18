package edu.univ.erp.ui;

import edu.univ.erp.service.AuthService;
import edu.univ.erp.domain.User;
import edu.univ.erp.ui.student.StudentPanel;
import edu.univ.erp.ui.instructor.InstructorPanel;
import edu.univ.erp.ui.admin.AdminPanel;
import com.formdev.flatlaf.FlatLightLaf;
import org.mindrot.jbcrypt.BCrypt;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private AuthService authService;

    public LoginFrame() {
        initializeAuth();
        setupUI();
    }

    private void initializeAuth() {
        try {
            authService = new AuthService();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Database connection failed: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void setupUI() {
        setTitle("University ERP - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header
        JLabel headerLabel = new JLabel("Welcome", JLabel.CENTER);
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        mainPanel.add(headerLabel, BorderLayout.NORTH);

        // Login form
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));

        formPanel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        formPanel.add(usernameField);

        formPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        formPanel.add(passwordField);

        JButton loginBtn = new JButton("Login");
        JButton cancelBtn = new JButton("Cancel");

        formPanel.add(loginBtn);
        formPanel.add(cancelBtn);

        mainPanel.add(formPanel, BorderLayout.CENTER);


        // Event handlers
        loginBtn.addActionListener(e -> performLogin());
        cancelBtn.addActionListener(e -> System.exit(0));

        // Enter key support
        passwordField.addActionListener(e -> performLogin());

        add(mainPanel);
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        System.out.println("Login attempted - Username: " + username + ", Password: " + password);

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Validation failed - empty fields");
            JOptionPane.showMessageDialog(this,
                    "Please enter both username and password",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            System.out.println("Calling authService.login...");
            User user = authService.login(username, password);
            System.out.println("Login successful - User: " + user.getUsername() + ", Role: " + user.getRole());

            openDashboard(user);
            dispose(); // Close login window
        } catch (Exception e) {
            System.out.println("Login failed with error: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    e.getMessage(),
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
            usernameField.requestFocus();
        }
    }

    private void openDashboard(User user) {
        switch (user.getRole()) {
            case "STUDENT":
                new StudentPanel(user).setVisible(true);
                break;
            case "INSTRUCTOR":
                new InstructorPanel(user).setVisible(true);
                break;
            case "ADMIN":
                new AdminPanel(user).setVisible(true);
                break;
            default:
                JOptionPane.showMessageDialog(this,
                        "Unknown user role: " + user.getRole(),
                        "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}