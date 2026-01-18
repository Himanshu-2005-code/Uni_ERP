package edu.univ.erp.ui.student;

import edu.univ.erp.domain.User;
import edu.univ.erp.domain.Student;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Grade;
import edu.univ.erp.service.StudentService;
import edu.univ.erp.service.MaintenanceService;
import edu.univ.erp.ui.DashboardFrame;
import edu.univ.erp.util.CsvExport;
import edu.univ.erp.util.PdfExport;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class StudentPanel extends DashboardFrame {
    private StudentService studentService;
    private MaintenanceService maintenanceService;
    private Student student;
    private User currentUser;

    // UI Components
    private JTable catalogTable;
    private JTable enrollmentsTable;
    private JTable gradesTable;
    private JTextArea timetableArea;

    public StudentPanel(User user) {
        super(user, "Student Dashboard");
        this.currentUser = user;
        this.studentService = new StudentService();
        this.maintenanceService = new MaintenanceService();
        initializeStudentData();
        setupUI();
        loadData();
    }

    private void initializeStudentData() {
        try {
            this.student = studentService.getStudentProfile(currentUser.getUserId());
            if (student == null) {
                showError("Failed to load student profile");
                System.exit(1);
            }
        } catch (Exception e) {
            showError("Failed to load student profile: " + e.getMessage());
            System.exit(1);
        }
    }

    private void setupUI() {
        //Sidebar buttons
        addSidebarButton("Course Catalog", "catalog");
        addSidebarButton("My Enrollments", "enrollments");
        addSidebarButton("My Grades", "grades");
        addSidebarButton("Timetable", "timetable");
        addSidebarButton("Transcript", "transcript");

        //Content panels
        contentPanel.add(createCatalogPanel(), "catalog");
        contentPanel.add(createEnrollmentsPanel(), "enrollments");
        contentPanel.add(createGradesPanel(), "grades");
        contentPanel.add(createTimetablePanel(), "timetable");
        contentPanel.add(createTranscriptPanel(), "transcript");

        // Show first panel by default
        cardLayout.show(contentPanel, "catalog");
    }

    private JPanel createCatalogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title
        JLabel titleLabel = new JLabel("Available Courses & Sections");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("Refresh");
        JButton enrollBtn = new JButton("Enroll in Selected Section");

        refreshBtn.addActionListener(e -> loadCatalog());
        enrollBtn.addActionListener(e -> enrollInSelectedSection());

        toolbar.add(refreshBtn);
        toolbar.add(enrollBtn);

        // Table with selection
        String[] columns = {"Select", "Section ID", "Course Code", "Course Title", "Credits",
                "Instructor", "Day/Time", "Room", "Capacity", "Available Seats"};
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

        catalogTable = new JTable(model);
        catalogTable.setRowHeight(25);
        catalogTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        catalogTable.getColumnModel().getColumn(1).setPreferredWidth(80);

        JScrollPane scrollPane = new JScrollPane(catalogTable);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createEnrollmentsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("My Current Enrollments");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("Refresh");
        JButton dropBtn = new JButton("Drop Selected Section");

        refreshBtn.addActionListener(e -> loadEnrollments());
        dropBtn.addActionListener(e -> dropSelectedSection());

        toolbar.add(refreshBtn);
        toolbar.add(dropBtn);

        // Table with selection
        String[] columns = {"Select", "Enrollment ID", "Course Code", "Course Title", "Instructor",
                "Day/Time", "Room", "Enrollment Date"};
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

        enrollmentsTable = new JTable(model);
        enrollmentsTable.setRowHeight(25);
        enrollmentsTable.getColumnModel().getColumn(0).setPreferredWidth(50);

        JScrollPane scrollPane = new JScrollPane(enrollmentsTable);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createGradesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("My Grades");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("Refresh");
        JButton exportCsvBtn = new JButton("Export CSV");
        JButton exportPdfBtn = new JButton("Export PDF");

        refreshBtn.addActionListener(e -> loadGrades());
        exportCsvBtn.addActionListener(e -> exportGradesCsv());
        exportPdfBtn.addActionListener(e -> exportGradesPdf());

        toolbar.add(refreshBtn);
        toolbar.add(exportCsvBtn);
        toolbar.add(exportPdfBtn);

        // Table
        String[] columns = {"Course", "Component", "Score", "Max Score", "Percentage", "Weight"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        gradesTable = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(gradesTable);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTimetablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("My Weekly Timetable");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);

        timetableArea = new JTextArea();
        timetableArea.setEditable(false);
        timetableArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        timetableArea.setBackground(new Color(250, 250, 250));

        JScrollPane scrollPane = new JScrollPane(timetableArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTranscriptPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Academic Transcript");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton generatePdfBtn = new JButton("Generate PDF Transcript");
        JButton generateCsvBtn = new JButton("Generate CSV Transcript");

        generatePdfBtn.addActionListener(e -> generateTranscriptPdf());
        generateCsvBtn.addActionListener(e -> generateTranscriptCsv());

        content.add(generatePdfBtn);
        content.add(generateCsvBtn);

        panel.add(content, BorderLayout.CENTER);

        return panel;
    }

    private void loadData() {
        loadCatalog();
        loadEnrollments();
        loadGrades();
        loadTimetable();
    }

    private void loadCatalog() {
        try {
            if (!maintenanceService.canUserModifyData(currentUser.getRole())) {
                showWarning("System is in maintenance mode. Viewing only.");
            }

            List<Section> sections = studentService.getAvailableSections();
            DefaultTableModel model = (DefaultTableModel) catalogTable.getModel();
            model.setRowCount(0);

            for (Section section : sections) {
                model.addRow(new Object[]{
                        false, // Selection checkbox
                        section.getSectionId(),
                        section.getCourseCode(),
                        section.getCourseTitle(),
                        "4",
                        section.getInstructorName(),
                        section.getDayTime(),
                        section.getRoom(),
                        section.getCapacity(),
                        section.getAvailableSeats()
                });
            }
        } catch (Exception e) {
            showError("Failed to load course catalog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadEnrollments() {
        try {
            List<Enrollment> enrollments = studentService.getStudentEnrollments(student.getUserId());
            DefaultTableModel model = (DefaultTableModel) enrollmentsTable.getModel();
            model.setRowCount(0);

            for (Enrollment enrollment : enrollments) {
                if (enrollment.isActive()) {
                    Section section = enrollment.getSection();
                    model.addRow(new Object[]{
                            false, // Selection checkbox
                            enrollment.getEnrollmentId(),
                            section.getCourseCode(),
                            section.getCourseTitle(),
                            section.getInstructorName(),
                            section.getDayTime(),
                            section.getRoom(),
                            enrollment.getEnrollmentDate().toString()
                    });
                }
            }
        } catch (Exception e) {
            showError("Failed to load enrollments: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadGrades() {
        try {
            List<Grade> grades = studentService.getStudentGrades(student.getUserId());
            List<Enrollment> enrollments = studentService.getStudentEnrollments(student.getUserId());

            DefaultTableModel model = (DefaultTableModel) gradesTable.getModel();
            model.setRowCount(0);
            for (Enrollment enrollment : enrollments) {

                for (Grade grade : grades) {
                    if (enrollment.getEnrollmentId() == grade.getEnrollmentId()) {
                        Section section = enrollment.getSection();

                        model.addRow(new Object[]{
                                section.getCourseCode(),
                                grade.getComponent(),
                                grade.getScore(),
                                grade.getMaxScore(),
                                String.format("%.1f%%", grade.getPercentage()),
                                String.format("%.2f%%", grade.getWeight())
                        });

                    }
                }
            }
        } catch (Exception e) {
            showError("Failed to load grades: " + e.getMessage());
            e.printStackTrace();
        }
    }
    private void loadTimetable() {
        try {
            List<Enrollment> enrollments = studentService.getStudentEnrollments(student.getUserId());
            StringBuilder sb = new StringBuilder();
            sb.append("MY WEEKLY TIMETABLE\n");
            sb.append("===================\n\n");

            for (Enrollment enrollment : enrollments) {
                if (enrollment.isActive()) {
                    Section section = enrollment.getSection();
                    sb.append(String.format("Course: %s - %s\n",
                            section.getCourseCode(), section.getCourseTitle()));
                    sb.append(String.format("Time: %s\n", section.getDayTime()));
                    sb.append(String.format("Room: %s\n", section.getRoom()));
                    sb.append(String.format("Instructor: %s\n", section.getInstructorName()));
                    sb.append("------------------------\n");
                }
            }

            if (enrollments.isEmpty()) {
                sb.append("No courses enrolled.\n");
                sb.append("Go to Course Catalog to enroll in courses.");
            }

            if (timetableArea != null) {
                timetableArea.setText(sb.toString());
            }
        } catch (Exception e) {
            showError("Failed to load timetable: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void enrollInSelectedSection() {
        if (!maintenanceService.canUserModifyData(currentUser.getRole())) {
            showError("Cannot enroll: System is in maintenance mode");
            return;
        }

        DefaultTableModel model = (DefaultTableModel) catalogTable.getModel();
        int selectedRow = -1;

        // Find the selected row
        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean selected = (Boolean) model.getValueAt(i, 0);
            if (selected != null && selected) {
                selectedRow = i;
                break;
            }
        }

        if (selectedRow == -1) {
            showWarning("Please select a section to enroll in (check the Select box)");
            return;
        }

        int sectionId = (Integer) model.getValueAt(selectedRow, 1);
        String courseCode = (String) model.getValueAt(selectedRow, 2);
        String courseName = (String) model.getValueAt(selectedRow, 3);
        String instructor = (String) model.getValueAt(selectedRow, 5);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to enroll in:\n" +
                        "Course: " + courseName + " (" + courseCode + ")\n" +
                        "Instructor: " + instructor + "\n" +
                        "Section ID: " + sectionId,
                "Confirm Enrollment", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            
            try {

                boolean success = studentService.enrollInSection(student.getUserId(), sectionId);

                if (success) {
                    showSuccess("Successfully enrolled in " + courseName);
                    // Clear selection
                    model.setValueAt(false, selectedRow, 0);
                    // Refresh all data
                    loadCatalog();
                    loadEnrollments();
                    loadTimetable();
                } else {
                    showError("Failed to enroll in " + courseName);
                }
            } catch (Exception e) {
                showError("Failed to enroll: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void dropSelectedSection() {
        if (!maintenanceService.canUserModifyData(currentUser.getRole())) {
            showError("Cannot drop: System is in maintenance mode");
            return;
        }

        DefaultTableModel model = (DefaultTableModel) enrollmentsTable.getModel();
        int selectedRow = -1;
        int enrollmentId = -1;

        // Find the selected row
        for (int i = 0; i < model.getRowCount(); i++) {
            Boolean selected = (Boolean) model.getValueAt(i, 0);
            if (selected != null && selected) {
                selectedRow = i;
                enrollmentId = (Integer) model.getValueAt(i, 1);
                break;
            }
        }

        if (selectedRow == -1) {
            showWarning("Please select an enrollment to drop (check the Select box)");
            return;
        }

        String courseName = (String) model.getValueAt(selectedRow, 3);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to drop: " + courseName + "?",
                "Confirm Drop", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean success = studentService.dropSection(enrollmentId);
                if (success) {
                    showSuccess("Successfully dropped " + courseName);
                    // Clear selection
                    model.setValueAt(false, selectedRow, 0);
                    // Refresh all data
                    loadCatalog();
                    loadEnrollments();
                    loadTimetable();
                } else {
                    showError("Failed to drop " + courseName);
                }
            } catch (Exception e) {
                showError("Failed to drop section: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void exportGradesCsv() {
        try {
            List<Grade> grades = studentService.getStudentGrades(student.getUserId());
            CsvExport.exportGrades(grades, student.getUsername() + "_grades.csv");
            showSuccess("Grades exported to CSV successfully");
        } catch (Exception e) {
            showError("Failed to export CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void exportGradesPdf() {
        try {
            List<Grade> grades = studentService.getStudentGrades(student.getUserId());
            PdfExport.exportTranscript(student, grades, student.getUsername() + "_transcript.pdf");
            showSuccess("Transcript exported to PDF successfully");
        } catch (Exception e) {
            showError("Failed to export PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generateTranscriptPdf() {
        exportGradesPdf();
    }

    private void generateTranscriptCsv() {
        exportGradesCsv();
    }
}