package edu.univ.erp.ui.instructor;

import edu.univ.erp.domain.User;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.domain.Student;
import edu.univ.erp.service.InstructorService;
import edu.univ.erp.service.MaintenanceService;
import edu.univ.erp.ui.DashboardFrame;
import edu.univ.erp.db.Database;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class InstructorPanel extends DashboardFrame {
    private InstructorService instructorService;
    private MaintenanceService maintenanceService;
    private JTable sectionsTable;
    private JTable gradesTable;
    private User currentUser;
    private int currentSectionId;
    private String currentCourseName;

    public InstructorPanel(User user) {
        super(user, "Instructor Dashboard");
        this.currentUser = user;
        this.instructorService = new InstructorService();
        this.maintenanceService = new MaintenanceService();
        setupUI();
        loadSections();
    }

    private void setupUI() {
        addSidebarButton("My Sections", "sections");
        addSidebarButton("Grade Management", "grades");
        addSidebarButton("Class Statistics", "stats");
        addSidebarButton("Export Grades", "export");

        contentPanel.add(createSectionsPanel(), "sections");
        contentPanel.add(createGradesPanel(), "grades");
        contentPanel.add(createStatsPanel(), "stats");
        contentPanel.add(createExportPanel(), "export");

        cardLayout.show(contentPanel, "sections");
    }

    private JPanel createSectionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("My Teaching Sections");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("Refresh");
        JButton manageGradesBtn = new JButton("Manage Grades");
        JButton updateEnrollmentBtn = new JButton("Update Enrollment Counts");

        refreshBtn.addActionListener(e -> loadSections());
        manageGradesBtn.addActionListener(e -> manageGrades());
        updateEnrollmentBtn.addActionListener(e -> updateAllEnrollmentCounts());

        toolbar.add(refreshBtn);
        toolbar.add(manageGradesBtn);
        toolbar.add(updateEnrollmentBtn);

        String[] columns = {"Select", "Section ID", "Course Code", "Course Title", "Day/Time",
                "Room", "Capacity", "Enrolled", "Available", "Semester", "Year"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? Boolean.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0; // Only the select column is editable
            }
        };

        sectionsTable = new JTable(model);
        sectionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(sectionsTable);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createGradesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Grade Management - " + (currentCourseName != null ? currentCourseName : "Select Section"));
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton loadStudentsBtn = new JButton("Load Students");
        JButton enterGradesBtn = new JButton("Save Grades");
        JButton computeFinalBtn = new JButton("Compute Final Grades");
        JButton refreshGradesBtn = new JButton("Refresh");
        JButton backBtn = new JButton("Back to Sections");
        JButton helpBtn = new JButton("Help");

        loadStudentsBtn.addActionListener(e -> loadStudentsForSection());
        enterGradesBtn.addActionListener(e -> enterGrades());
        computeFinalBtn.addActionListener(e -> computeFinalGrades());
        refreshGradesBtn.addActionListener(e -> refreshGrades());
        backBtn.addActionListener(e -> cardLayout.show(contentPanel, "sections"));
        helpBtn.addActionListener(e -> showGradeEntryInstructions());

        toolbar.add(loadStudentsBtn);
        toolbar.add(enterGradesBtn);
        toolbar.add(computeFinalBtn);
        toolbar.add(refreshGradesBtn);
        toolbar.add(backBtn);
        toolbar.add(helpBtn);

        String[] gradeColumns = {"Enrollment ID", "Student ID", "Username", "Roll No",
                "Quiz (0-20)", "Midterm (0-30)", "Final (0-50)", "Final Grade"};
        DefaultTableModel gradeModel = new DefaultTableModel(gradeColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 4 && column <= 6;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column >= 4 && column <= 6) return Double.class;
                return String.class;
            }
        };

        gradesTable = new JTable(gradeModel);
        gradesTable.setRowHeight(25);
        gradesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        gradesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        JScrollPane gradeScrollPane = new JScrollPane(gradesTable);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(gradeScrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Class Statistics");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);

        JTextArea statsArea = new JTextArea(15, 50);
        statsArea.setEditable(false);
        statsArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        statsArea.setBackground(new Color(250, 250, 250));

        JButton loadStatsBtn = new JButton("Load Statistics");
        loadStatsBtn.addActionListener(e -> loadStatistics());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(loadStatsBtn);

        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(statsArea), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createExportPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Export Grades");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton exportCsvBtn = new JButton("Export CSV");
        JButton backBtn = new JButton("Back to Sections");

        exportCsvBtn.addActionListener(e -> exportGradesCsv());
        backBtn.addActionListener(e -> cardLayout.show(contentPanel, "sections"));

        content.add(exportCsvBtn);
        content.add(backBtn);

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private void loadSections() {
        try {
            List<Section> sections = instructorService.getInstructorSections(currentUser.getUserId());
            DefaultTableModel model = (DefaultTableModel) sectionsTable.getModel();
            model.setRowCount(0);

            for (Section section : sections) {
                int availableSeats = section.getCapacity() - section.getEnrolledCount();

                //select column (false by default)
                model.addRow(new Object[]{
                        false,
                        section.getSectionId(),
                        section.getCourseCode(),
                        section.getCourseTitle(),
                        section.getDayTime(),
                        section.getRoom(),
                        section.getCapacity(),
                        section.getEnrolledCount(),
                        availableSeats,
                        section.getSemester(),
                        section.getYear()
                });
            }

            if (sections.isEmpty()) {
                showInfo("No teaching sections found for current semester.");
            } else {
                showSuccess("Loaded " + sections.size() + " sections with updated enrollment counts");
            }
        } catch (Exception e) {
            showError("Failed to load sections: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateAllEnrollmentCounts() {
        try {
            List<Section> sections = instructorService.getInstructorSections(currentUser.getUserId());
            int updatedCount = 0;

            for (Section section : sections) {
                if (instructorService.updateEnrollmentCount(section.getSectionId())) {
                    updatedCount++;
                }
            }

            showSuccess("Updated enrollment counts for " + updatedCount + " sections");
            loadSections();
        } catch (Exception e) {
            showError("Failed to update enrollment counts: " + e.getMessage());
        }
    }

    private void manageGrades() {
        DefaultTableModel model = (DefaultTableModel) sectionsTable.getModel();
        int selectedRow = -1;

        // Finding the selected row (checkbox checked)
        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean selected = (Boolean) model.getValueAt(i, 0);
            if (selected != null && selected) {
                selectedRow = i;
                break;
            }
        }

        if (selectedRow == -1) {
            showWarning("Please select a section to manage grades (check the Select box)");
            return;
        }

        currentSectionId = (Integer) model.getValueAt(selectedRow, 1); // Column 1 is Section ID
        currentCourseName = (String) model.getValueAt(selectedRow, 3); // Column 3 is Course Title

        // Clear the selection after use
        model.setValueAt(false, selectedRow, 0);

        cardLayout.show(contentPanel, "grades");
        showSuccess("Now managing grades for: " + currentCourseName + " (Section: " + currentSectionId + ")");

        loadStudentsForSection();
    }

    private void loadStudentsForSection() {
        if (currentSectionId == 0) {
            showWarning("Please select a section first");
            return;
        }

        try {
            List<Enrollment> enrollments = instructorService.getSectionEnrollments(currentSectionId);
            Map<Integer, Map<String, Double>> currentGrades = instructorService.getCurrentGrades(currentSectionId);

            DefaultTableModel model = (DefaultTableModel) gradesTable.getModel();
            model.setRowCount(0);

            for (Enrollment enrollment : enrollments) {
                Student student = instructorService.getStudentDetails(enrollment.getStudentId());
                Map<String, Double> grades = currentGrades.get(enrollment.getEnrollmentId());

                double quizScore = (grades != null && grades.containsKey("quiz")) ? grades.get("quiz") : 0.0;
                double midtermScore = (grades != null && grades.containsKey("midterm")) ? grades.get("midterm") : 0.0;
                double finalScore = (grades != null && grades.containsKey("final")) ? grades.get("final") : 0.0;

                String finalGradeDisplay;


                // BUG: Check if both final_grade and final_percentage exist in database
                boolean hasFinalGrade = enrollment.getFinalGrade() != null && !enrollment.getFinalGrade().trim().isEmpty();
                boolean hasFinalPercentage = enrollment.getFinalPercentage() > 0;

                if (hasFinalGrade && hasFinalPercentage) {
                    // Use the stored final grade and percentage from database
                    finalGradeDisplay = String.format("%.1f%% (%s)", enrollment.getFinalPercentage(), enrollment.getFinalGrade());
                } else {
                    // Showing the current total points if no final grade calculated
                    double currentPercentage = quizScore + midtermScore + finalScore;
                    finalGradeDisplay = currentPercentage > 0 ? String.format("%.1f%% (Not Calculated)", currentPercentage) : "Not Calculated";
                }

                model.addRow(new Object[]{
                        enrollment.getEnrollmentId(),
                        enrollment.getStudentId(),
                        student != null ? student.getUsername() : "Unknown",
                        student != null ? student.getRollNo() : "N/A",
                        quizScore,
                        midtermScore,
                        finalScore,
                        finalGradeDisplay
                });
            }
            showSuccess("Loaded " + enrollments.size() + " students with current grades");

        } catch (Exception e) {
            showError("Failed to load students: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void refreshGrades() {
        loadStudentsForSection();
    }

    private void enterGrades() {
        if (currentSectionId == 0) {
            showWarning("Please select a section first");
            return;
        }

        if (!maintenanceService.canUserModifyData(currentUser.getRole())) {
            showError("Cannot modify grades: System is in maintenance mode");
            return;
        }

        try {
            DefaultTableModel model = (DefaultTableModel) gradesTable.getModel();
            Map<Integer, Map<String, Double>> grades = new HashMap<>();

            int savedCount = 0;
            for (int i = 0; i < model.getRowCount(); i++) {
                int enrollmentId = (Integer) model.getValueAt(i, 0);
                Map<String, Double> studentGrades = new HashMap<>();

                Object quizObj = model.getValueAt(i, 4);
                if (quizObj != null && !quizObj.toString().isEmpty()) {
                    try {
                        double quizScore = Double.parseDouble(quizObj.toString());
                        if (quizScore >= 0 && quizScore <= 20) {
                            studentGrades.put("quiz", quizScore);
                        } else {
                            showWarning("Quiz score must be between 0-20 for student at row " + (i + 1));
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        showWarning("Invalid quiz score format for student at row " + (i + 1));
                        continue;
                    }
                }

                Object midtermObj = model.getValueAt(i, 5);
                if (midtermObj != null && !midtermObj.toString().isEmpty()) {
                    try {
                        double midtermScore = Double.parseDouble(midtermObj.toString());
                        if (midtermScore >= 0 && midtermScore <= 30) {
                            studentGrades.put("midterm", midtermScore);
                        } else {
                            showWarning("Midterm score must be between 0-30 for student at row " + (i + 1));
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        showWarning("Invalid midterm score format for student at row " + (i + 1));
                        continue;
                    }
                }

                Object finalObj = model.getValueAt(i, 6);
                if (finalObj != null && !finalObj.toString().isEmpty()) {
                    try {
                        double finalScore = Double.parseDouble(finalObj.toString());
                        if (finalScore >= 0 && finalScore <= 50) {
                            studentGrades.put("final", finalScore);
                        } else {
                            showWarning("Final score must be between 0-50 for student at row " + (i + 1));
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        showWarning("Invalid final score format for student at row " + (i + 1));
                        continue;
                    }
                }

                if (!studentGrades.isEmpty()) {
                    grades.put(enrollmentId, studentGrades);
                    savedCount++;
                }
            }

            if (grades.isEmpty()) {
                showWarning("No valid grades to save. Please enter scores in the appropriate columns.");
                return;
            }

            boolean success = instructorService.saveGrades(currentSectionId, grades);
            if (success) {
                showSuccess("Grades saved successfully for " + savedCount + " students");
                loadStudentsForSection();
            } else {
                showError("Failed to save grades");
            }
        } catch (Exception e) {
            showError("Failed to enter grades: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void computeFinalGrades() {
        if (currentSectionId == 0) {
            showWarning("Please select a section first");
            return;
        }

        if (!maintenanceService.canUserModifyData(currentUser.getRole())) {
            showError("Cannot compute grades: System is in maintenance mode");
            return;
        }

        try {
            JTextField quizWeightField = new JTextField("20");
            JTextField midtermWeightField = new JTextField("30");
            JTextField finalWeightField = new JTextField("50");

            JPanel weightPanel = new JPanel(new GridLayout(3, 2, 10, 10));
            weightPanel.add(new JLabel("Quiz Weight (%):"));
            weightPanel.add(quizWeightField);
            weightPanel.add(new JLabel("Midterm Weight (%):"));
            weightPanel.add(midtermWeightField);
            weightPanel.add(new JLabel("Final Weight (%):"));
            weightPanel.add(finalWeightField);

            int result = JOptionPane.showConfirmDialog(this, weightPanel,
                    "Configure Grade Weights", JOptionPane.OK_CANCEL_OPTION);

            if (result != JOptionPane.OK_OPTION) {
                return;
            }

            double quizWeight = Double.parseDouble(quizWeightField.getText()) / 100.0;
            double midtermWeight = Double.parseDouble(midtermWeightField.getText()) / 100.0;
            double finalWeight = Double.parseDouble(finalWeightField.getText()) / 100.0;

            double totalWeight = quizWeight + midtermWeight + finalWeight;
            if (Math.abs(totalWeight - 1.0) > 0.01) {
                showError("Weights must sum to 100%. Current total: " + (totalWeight * 100) + "%");
                return;
            }


            // Calculating grades directly in the UI to ensure correctness
            DefaultTableModel model = (DefaultTableModel) gradesTable.getModel();
            int updatedCount = 0;

            for (int i = 0; i < model.getRowCount(); i++) {
                int enrollmentId = (Integer) model.getValueAt(i, 0);

                // Get scores from table
                Object quizObj = model.getValueAt(i, 4);
                Object midtermObj = model.getValueAt(i, 5);
                Object finalObj = model.getValueAt(i, 6);

                double quizScore = 0.0, midtermScore = 0.0, finalScore = 0.0;
                boolean hasAllScores = true;

                // Parse quiz score
                if (quizObj != null && !quizObj.toString().isEmpty()) {
                    quizScore = Double.parseDouble(quizObj.toString());
                } else {
                    hasAllScores = false;
                }

                // Parse midterm score
                if (midtermObj != null && !midtermObj.toString().isEmpty()) {
                    midtermScore = Double.parseDouble(midtermObj.toString());
                } else {
                    hasAllScores = false;
                }

                // Parse final score
                if (finalObj != null && !finalObj.toString().isEmpty()) {
                    finalScore = Double.parseDouble(finalObj.toString());
                } else {
                    hasAllScores = false;
                }

                if (hasAllScores) {
                    double finalPercentageTotal = calculateFinalPercentage(quizScore, midtermScore, finalScore, quizWeight, midtermWeight, finalWeight);
                    String finalGrade = calculateLetterGrade(finalPercentageTotal);

                    // Update the table with both values
                    String combinedGrade = String.format("%.1f%% (%s)", finalPercentageTotal, finalGrade);
                    model.setValueAt(combinedGrade, i, 7); // Column 7 is Final Grade
                    updatedCount++;

                    
                    boolean dbSuccess = updateGradeInDatabasePermanently(enrollmentId, finalGrade, finalPercentageTotal);
                    if (dbSuccess) {
                        System.out.println("Stored both in database: " + finalPercentageTotal + "% (" + finalGrade + ")");
                    } else {
                        System.out.println("Failed to store in database for enrollment " + enrollmentId);
                    }
                } else {
                    System.out.println("Skipping student " + enrollmentId + " - missing scores");
                }
            }

            if (updatedCount > 0) {
                showSuccess("Final grades computed for " + updatedCount + " students using weights: " +
                        "Quiz=" + (quizWeight * 100) + "%, " +
                        "Midterm=" + (midtermWeight * 100) + "%, " +
                        "Final=" + (finalWeight * 100) + "%");

                // Refresh to show updated data from database

            } else {
                showWarning("No final grades could be computed. Make sure all students have all scores entered.");
            }
        } catch (NumberFormatException e) {
            showError("Please enter valid numbers for weights and ensure all scores are numbers");
        } catch (Exception e) {
            showError("Failed to compute final grades: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //Calculate final percentage
    private double calculateFinalPercentage(double quizScore, double midtermScore, double finalScore,
                                            double quizWeight, double midtermWeight, double finalWeight) {

        // Quiz: 20 points max, Midterm: 30 points max, Final: 50 points max

        double quizPercentage = (quizScore / 20.0) * 100;      
        double midtermPercentage = (midtermScore / 30.0) * 100; 
        double finalPercentage = (finalScore / 50.0) * 100;     

        // Apply weights
        double weightedQuiz = quizPercentage * quizWeight;
        double weightedMidterm = midtermPercentage * midtermWeight;
        double weightedFinal = finalPercentage * finalWeight;

        double finalPercentageTotal = weightedQuiz + weightedMidterm + weightedFinal;
        return Math.round(finalPercentageTotal * 10.0) / 10.0; // Round to 1 decimal
    }

    private String calculateLetterGrade(double percentage) {

        if (percentage >= 90) {
            return "A";
        } else if (percentage >= 80) {
            return "B";
        } else if (percentage >= 70) {
            return "C";
        } else if (percentage >= 60) {
            return "D";
        } else {
            return "F";
        }
    }

    private boolean updateGradeInDatabasePermanently(int enrollmentId, String finalGrade, double finalPercentage) {
        try {
            // First ensure the final_percentage column exists
            ensureFinalPercentageColumnExists();

            String sql = "UPDATE enrollments SET final_grade = ?, final_percentage = ? WHERE enrollment_id = ?";

            try (Connection conn = Database.getERPConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, finalGrade);
                stmt.setDouble(2, finalPercentage);
                stmt.setInt(3, enrollmentId);
                int rowsUpdated = stmt.executeUpdate();

                return rowsUpdated > 0;
            }
        } catch (Exception e) {
            System.err.println("ERROR - Failed to update database for enrollment " + enrollmentId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void ensureFinalPercentageColumnExists() {
        try {
            String checkSql = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'enrollments' AND COLUMN_NAME = 'final_percentage'";

            String alterSql = "ALTER TABLE enrollments ADD COLUMN final_percentage DECIMAL(5,2)";

            try (Connection conn = Database.getERPConnection();
                 PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                 ResultSet rs = checkStmt.executeQuery()) {

                if (rs.next() && rs.getInt(1) == 0) {
                    try (PreparedStatement alterStmt = conn.prepareStatement(alterSql)) {
                        alterStmt.executeUpdate();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("ERROR - Failed to ensure final_percentage column exists: " + e.getMessage());
        }
    }

    private void loadStatistics() {
        if (currentSectionId == 0) {
            showWarning("Please select a section first");
            return;
        }

        try {
            Map<String, Object> stats = instructorService.computeSectionStats(currentSectionId);

            Component[] components = ((JPanel) contentPanel.getComponent(2)).getComponents();
            JTextArea statsArea = null;
            for (Component comp : components) {
                if (comp instanceof JScrollPane) {
                    JViewport viewport = ((JScrollPane) comp).getViewport();
                    Component view = viewport.getView();
                    if (view instanceof JTextArea) {
                        statsArea = (JTextArea) view;
                        break;
                    }
                }
            }

            if (statsArea != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("CLASS STATISTICS\n");
                sb.append("================\n\n");
                sb.append("Section: ").append(currentCourseName).append(" (ID: ").append(currentSectionId).append(")\n");
                sb.append("Total Students: ").append(stats.get("totalStudents")).append("\n");
                sb.append("Students with Grades: ").append(stats.get("studentsWithGrades")).append("\n");
                sb.append("Capacity: ").append(stats.get("capacity")).append("\n");
                sb.append("Currently Enrolled: ").append(stats.get("enrolledCount")).append("\n");
                sb.append("Available Seats: ").append(stats.get("availableSeats")).append("\n\n");

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> componentStats = (List<Map<String, Object>>) stats.get("componentStats");

                if (componentStats != null && !componentStats.isEmpty()) {
                    for (Map<String, Object> component : componentStats) {
                        sb.append(component.get("component").toString().toUpperCase()).append(" STATISTICS:\n");
                        sb.append("  Average Score: ").append(String.format("%.2f", component.get("average"))).append("\n");
                        sb.append("  Maximum Score: ").append(String.format("%.2f", component.get("maximum"))).append("\n");
                        sb.append("  Minimum Score: ").append(String.format("%.2f", component.get("minimum"))).append("\n");
                        sb.append("  Standard Deviation: ").append(String.format("%.2f", component.get("stdDev"))).append("\n");
                        sb.append("  Students Graded: ").append(component.get("count")).append("\n\n");
                    }
                } else {
                    sb.append("No grade data available yet.\n");
                    sb.append("Enter grades first to see statistics.");
                }

                statsArea.setText(sb.toString());
            }
        } catch (Exception e) {
            showError("Failed to load statistics: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void exportGradesCsv() {
        if (currentSectionId == 0) {
            showWarning("Please select a section first");
            return;
        }

        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Grades as CSV");
            fileChooser.setSelectedFile(new java.io.File("section_" + currentSectionId + "_grades.csv"));

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                String filename = fileChooser.getSelectedFile().getAbsolutePath();
                if (!filename.toLowerCase().endsWith(".csv")) {
                    filename += ".csv";
                }

                boolean success = instructorService.exportGradesToCsv(currentSectionId, filename);
                if (success) {
                    showSuccess("Grades exported successfully to: " + filename);
                } else {
                    showError("Failed to export grades");
                }
            }
        } catch (Exception e) {
            showError("Failed to export grades: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showGradeEntryInstructions() {
        String instructions =
                "GRADE ENTRY INSTRUCTIONS:\n\n" +
                        "1. Enter scores directly in the table columns:\n" +
                        "   - Quiz: 0-20 points\n" +
                        "   - Midterm: 0-30 points  \n" +
                        "   - Final: 0-50 points\n\n" +
                        "2. Click 'Save Grades' to save all entered scores\n" +
                        "3. Click 'Compute Final Grades' to calculate final grades\n" +
                        "4. Configure weights when computing final grades\n" +
                        "5. Final percentage and grade will be displayed\n\n" +
                        "Example: Quiz=20%, Midterm=30%, Final=50%";

        JOptionPane.showMessageDialog(this, instructions, "Grade Entry Help", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

}