package edu.univ.erp.ui;

import edu.univ.erp.domain.User;
import edu.univ.erp.service.MaintenanceService;
import javax.swing.*;
import java.awt.*;

public class DashboardFrame extends JFrame {
    protected User currentUser;
    protected JPanel mainPanel;
    protected JPanel sidebarPanel;
    protected JPanel contentPanel;
    protected CardLayout cardLayout;
    protected MaintenanceService maintenanceService;

    public DashboardFrame(User user, String title) {
        this.currentUser = user;
        this.maintenanceService = new MaintenanceService();
        setTitle(title + " - University ERP");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 800);
        setLocationRelativeTo(null);

        setupMainPanel();
    }

    private void setupMainPanel() {
        mainPanel = new JPanel(new BorderLayout());

        // Create header with logout button
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Create sidebar and content area
        JSplitPane splitPane = createSplitPane();
        mainPanel.add(splitPane, BorderLayout.CENTER);

        add(mainPanel);
    }

    private JSplitPane createSplitPane() {
        // Create sidebar
        sidebarPanel = new JPanel();
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setBackground(new Color(240, 240, 240));
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        sidebarPanel.setPreferredSize(new Dimension(200, 0));

        // Create content panel with card layout
        contentPanel = new JPanel();
        cardLayout = new CardLayout();
        contentPanel.setLayout(cardLayout);

        // Create split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                sidebarPanel, contentPanel);
        splitPane.setDividerLocation(200);
        splitPane.setDividerSize(3);

        return splitPane;
    }

    protected void addSidebarButton(String name, String cardName) {
        JButton button = new JButton(name);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(180, 40));
        button.setFont(new Font("SansSerif", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));

        button.addActionListener(e -> cardLayout.show(contentPanel, cardName));

        sidebarPanel.add(button);
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    }

    protected void addSidebarSeparator() {
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setMaximumSize(new Dimension(180, 1));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebarPanel.add(separator);
        sidebarPanel.add(Box.createRigidArea(new Dimension(0, 10)));
    }

    protected JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(70, 130, 180));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        // Welcome message
        JLabel welcomeLabel = new JLabel(
                String.format("University ERP System - Welcome, %s (%s)",
                        currentUser.getUsername(), currentUser.getRole())
        );
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        // Maintenance status
        boolean maintenanceMode = maintenanceService.isMaintenanceMode();
        JLabel maintenanceLabel = new JLabel(
                maintenanceMode ? "🔧 MAINTENANCE MODE ACTIVE" : "✅ SYSTEM ACTIVE"
        );
        maintenanceLabel.setForeground(maintenanceMode ? Color.YELLOW : Color.GREEN);
        maintenanceLabel.setFont(new Font("SansSerif", Font.BOLD, 12));

        // Logout button
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(new Color(220, 80, 80));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.addActionListener(e -> logout());

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        statusPanel.setOpaque(false);
        statusPanel.add(maintenanceLabel);
        statusPanel.add(logoutBtn);

        headerPanel.add(welcomeLabel, BorderLayout.WEST);
        headerPanel.add(statusPanel, BorderLayout.EAST);

        return headerPanel;
    }

    protected void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            new edu.univ.erp.ui.LoginFrame().setVisible(true);
        }
    }

    protected void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    protected void showSuccess(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    protected void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }
}