package edu.univ.erp.ui.admin;

import edu.univ.erp.domain.User;
import edu.univ.erp.domain.Student;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.Section;
import edu.univ.erp.service.AdminService;
import edu.univ.erp.service.MaintenanceService;
import edu.univ.erp.service.UserManagementService;
import edu.univ.erp.service.AuthService;
import edu.univ.erp.ui.DashboardFrame;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;

public class AdminPanel extends DashboardFrame {
    private AdminService adminService;
    private MaintenanceService maintenanceService;
    private UserManagementService userManagementService;
    private AuthService authService;
    private JTable coursesTable;
    private JTable sectionsTable;
    private JCheckBox maintenanceCheckbox;
    private User currentUser;

    public AdminPanel(User user) {
        super(user, "Admin Dashboard");
        this.currentUser = user;
        this.adminService = new AdminService();
        this.maintenanceService = new MaintenanceService();
        this.userManagementService = new UserManagementService();
        this.authService = new AuthService();
        setupUI();
        loadCourses();
        loadMaintenanceStatus();
    }

    private void setupUI() {
        //sidebar buttons
        addSidebarButton("User Management", "users");
        addSidebarButton("Course Management", "courses");
        addSidebarButton("Section Management", "sections");
        addSidebarSeparator();
        addSidebarButton("System Settings", "settings");
        addSidebarButton("Backup/Restore", "backup");

        //content panels
        contentPanel.add(createUserManagementPanel(), "users");
        contentPanel.add(createCoursesPanel(), "courses");
        contentPanel.add(createSectionsPanel(), "sections");
        contentPanel.add(createSystemSettingsPanel(), "settings");
        contentPanel.add(createBackupPanel(), "backup");

        // Show first panel by default
        cardLayout.show(contentPanel, "users");
    }

    private JPanel createUserManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("User Management");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addStudentBtn = new JButton("Add Student");
        JButton addInstructorBtn = new JButton("Add Instructor");
        JButton addAdminBtn = new JButton("Add Admin");
        JButton viewAllUsersBtn = new JButton("View All Users");
        JButton viewStudentsBtn = new JButton("View Students");
        JButton viewInstructorsBtn = new JButton("View Instructors");

        addStudentBtn.addActionListener(e -> showAddStudentDialog());
        addInstructorBtn.addActionListener(e -> showAddInstructorDialog());
        addAdminBtn.addActionListener(e -> showAddAdminDialog());
        viewAllUsersBtn.addActionListener(e -> showAllUsers());
        viewStudentsBtn.addActionListener(e -> showStudents());
        viewInstructorsBtn.addActionListener(e -> showInstructors());

        toolbar.add(addStudentBtn);
        toolbar.add(addInstructorBtn);
        toolbar.add(addAdminBtn);
        toolbar.add(Box.createRigidArea(new Dimension(20, 0)));
        toolbar.add(viewAllUsersBtn);
        toolbar.add(viewStudentsBtn);
        toolbar.add(viewInstructorsBtn);

        panel.add(toolbar, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createCoursesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Course Management");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("Refresh");
        JButton addCourseBtn = new JButton("Add New Course");
        JButton deleteCourseBtn = new JButton("Delete Selected");
        JButton editCourseBtn = new JButton("Edit Selected");

        refreshBtn.addActionListener(e -> loadCourses());
        addCourseBtn.addActionListener(e -> showAddCourseDialog());
        deleteCourseBtn.addActionListener(e -> deleteSelectedCourse());
        editCourseBtn.addActionListener(e -> editSelectedCourse());

        toolbar.add(refreshBtn);
        toolbar.add(addCourseBtn);
        toolbar.add(editCourseBtn);
        toolbar.add(deleteCourseBtn);

        // Table with sorting
        String[] columns = {"Course ID", "Code", "Title", "Credits", "Description"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);

        coursesTable = new JTable(model);
        coursesTable.setRowHeight(25);

        // Enable sorting
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        coursesTable.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(coursesTable);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSectionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Section Management");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("Refresh");
        JButton addSectionBtn = new JButton("Add New Section");
        JButton viewSectionsBtn = new JButton("View All Sections");

        refreshBtn.addActionListener(e -> loadSections());
        addSectionBtn.addActionListener(e -> showAddSectionDialog());
        viewSectionsBtn.addActionListener(e -> showAllSections());

        toolbar.add(refreshBtn);
        toolbar.add(addSectionBtn);
        toolbar.add(viewSectionsBtn);

        // Sections table
        String[] columns = {"Section ID", "Course Code", "Course Title", "Instructor",
                "Day/Time", "Room", "Capacity", "Enrolled", "Semester", "Year"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);

        sectionsTable = new JTable(model);
        sectionsTable.setRowHeight(25);

        // Enable sorting
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        sectionsTable.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(sectionsTable);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSystemSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("System Settings");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridLayout(5, 1, 10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Maintenance mode
        JPanel maintenancePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        maintenanceCheckbox = new JCheckBox("Enable Maintenance Mode");
        JButton saveMaintenanceBtn = new JButton("Save");

        maintenancePanel.add(maintenanceCheckbox);
        maintenancePanel.add(saveMaintenanceBtn);

        // System info
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        content.add(maintenancePanel);
        content.add(infoPanel);
        content.add(new JLabel("Database Status: Connected"));
        content.add(new JLabel("Total Users: " + getTotalUsersCount()));

        // Event handlers
        saveMaintenanceBtn.addActionListener(e -> toggleMaintenanceMode());

        panel.add(content, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBackupPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Backup & Restore");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton backupBtn = new JButton("Backup Database");
        JButton restoreBtn = new JButton("Restore Database");

        backupBtn.addActionListener(e -> backupDatabase());
        restoreBtn.addActionListener(e -> restoreDatabase());

        content.add(backupBtn);
        content.add(restoreBtn);

        // Instructions
        JTextArea instructions = new JTextArea(
                "Backup Instructions:\n" +
                        "Click 'Backup Database' to create a backup\n" +
                        "Backups are saved as SQL files\n" +
                        "Restore from backup using 'Restore Database'\n"+
                        "In Restore you have to give sql file\n"
        );
        instructions.setEditable(false);
        instructions.setBackground(new Color(240, 240, 240));
        instructions.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(content, BorderLayout.NORTH);
        panel.add(new JScrollPane(instructions), BorderLayout.CENTER);

        return panel;
    }

    private void loadCourses() {
        try {
            List<Course> courses = adminService.getAllCourses();
            DefaultTableModel model = (DefaultTableModel) coursesTable.getModel();
            model.setRowCount(0);

            for (Course course : courses) {
                model.addRow(new Object[]{
                        course.getCourseId(),
                        course.getCode(),
                        course.getTitle(),
                        course.getCredits(),
                        course.getDescription()
                });
            }
        } catch (Exception e) {
            showError("Failed to load courses: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadSections() {
        try {
            List<Section> sections = adminService.getAllSections();
            DefaultTableModel model = (DefaultTableModel) sectionsTable.getModel();
            model.setRowCount(0);

            for (Section section : sections) {
                model.addRow(new Object[]{
                        section.getSectionId(),
                        section.getCourseCode(),
                        section.getCourseTitle(),
                        section.getInstructorName(),
                        section.getDayTime(),
                        section.getRoom(),
                        section.getCapacity(),
                        section.getEnrolledCount(),
                        section.getSemester(),
                        section.getYear()
                });
            }
        } catch (Exception e) {
            showError("Failed to load sections: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadMaintenanceStatus() {
        try {
            boolean isMaintenance = maintenanceService.isMaintenanceMode();
            maintenanceCheckbox.setSelected(isMaintenance);
        } catch (Exception e) {
            showError("Failed to load maintenance status: " + e.getMessage());
        }
    }

    private void deleteSelectedCourse() {
        int selectedRow = coursesTable.getSelectedRow();
        if (selectedRow == -1) {
            showWarning("Please select a course to delete");
            return;
        }

        // Convert view row index to model row index (for sorting)
        int modelRow = coursesTable.convertRowIndexToModel(selectedRow);
        DefaultTableModel model = (DefaultTableModel) coursesTable.getModel();

        int courseId = (Integer) model.getValueAt(modelRow, 0);
        String courseName = (String) model.getValueAt(modelRow, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete: " + courseName + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean success = adminService.deleteCourse(courseId);
                if (success) {
                    showSuccess("Course deleted successfully: " + courseName);
                    loadCourses(); // Refresh the table
                } else {
                    showError("Failed to delete course from database");
                }
            } catch (Exception e) {
                showError("Error deleting course: " + e.getMessage());
            }
        }
    }

    private void editSelectedCourse() {
        int selectedRow = coursesTable.getSelectedRow();
        if (selectedRow == -1) {
            showWarning("Please select a course to edit");
            return;
        }

        int modelRow = coursesTable.convertRowIndexToModel(selectedRow);
        DefaultTableModel model = (DefaultTableModel) coursesTable.getModel();

        int courseId = (Integer) model.getValueAt(modelRow, 0);
        String currentCode = (String) model.getValueAt(modelRow, 1);
        String currentTitle = (String) model.getValueAt(modelRow, 2);
        int currentCredits = (Integer) model.getValueAt(modelRow, 3);
        String currentDescription = (String) model.getValueAt(modelRow, 4);

        JTextField codeField = new JTextField(currentCode, 10);
        JTextField titleField = new JTextField(currentTitle, 20);
        JTextField creditsField = new JTextField(String.valueOf(currentCredits), 5);
        JTextArea descriptionArea = new JTextArea(currentDescription, 3, 20);
        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.add(new JLabel("Course Code:"));
        panel.add(codeField);
        panel.add(new JLabel("Course Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Credits:"));
        panel.add(creditsField);
        panel.add(new JLabel("Description:"));
        panel.add(descriptionScroll);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Edit Course", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String code = codeField.getText().trim();
                String title = titleField.getText().trim();
                int credits = Integer.parseInt(creditsField.getText().trim());
                String description = descriptionArea.getText().trim();

                if (code.isEmpty() || title.isEmpty()) {
                    showError("Please fill required fields");
                    return;
                }

                boolean success = adminService.updateCourse(courseId, code, title, credits, description);
                if (success) {
                    showSuccess("Course updated successfully");
                    loadCourses(); // Refresh the table
                } else {
                    showError("Failed to update course");
                }
            } catch (NumberFormatException e) {
                showError("Please enter valid credits");
            } catch (Exception e) {
                showError("Failed to update course: " + e.getMessage());
            }
        }
    }

    // User Management Dialogs
    private void showAddStudentDialog() {
        JTextField usernameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        JTextField rollNoField = new JTextField(15);
        JTextField programField = new JTextField(15);
        JTextField yearField = new JTextField(5);

        JPanel panel = new JPanel(new GridLayout(5, 2, 10, 10));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel("Roll Number:"));
        panel.add(rollNoField);
        panel.add(new JLabel("Program:"));
        panel.add(programField);
        panel.add(new JLabel("Year:"));
        panel.add(yearField);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Add New Student", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String username = usernameField.getText().trim();
                String password = new String(passwordField.getPassword());
                String rollNo = rollNoField.getText().trim();
                String program = programField.getText().trim();
                int year = Integer.parseInt(yearField.getText().trim());

                if (username.isEmpty() || password.isEmpty() || rollNo.isEmpty() || program.isEmpty()) {
                    showError("Please fill all fields");
                    return;
                }

                boolean success = userManagementService.createStudent(username, password, rollNo, program, year);
                if (success) {
                    showSuccess("Student created successfully");
                } else {
                    showError("Failed to create student");
                }
            } catch (NumberFormatException e) {
                showError("Please enter a valid number for year");
            } catch (Exception e) {
                showError("Failed to create student: " + e.getMessage());
            }
        }
    }

    private void showAddInstructorDialog() {
        JTextField usernameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        JTextField departmentField = new JTextField(15);
        JTextField officeField = new JTextField(15);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel("Department:"));
        panel.add(departmentField);
        panel.add(new JLabel("Office:"));
        panel.add(officeField);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Add New Instructor", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String username = usernameField.getText().trim();
                String password = new String(passwordField.getPassword());
                String department = departmentField.getText().trim();
                String office = officeField.getText().trim();

                if (username.isEmpty() || password.isEmpty() || department.isEmpty()) {
                    showError("Please fill all required fields");
                    return;
                }

                boolean success = userManagementService.createInstructor(username, password, department, office);
                if (success) {
                    showSuccess("Instructor created successfully");
                } else {
                    showError("Failed to create instructor");
                }
            } catch (Exception e) {
                showError("Failed to create instructor: " + e.getMessage());
            }
        }
    }

    private void showAddAdminDialog() {
        JTextField usernameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);

        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Add New Admin", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String username = usernameField.getText().trim();
                String password = new String(passwordField.getPassword());

                if (username.isEmpty() || password.isEmpty()) {
                    showError("Please fill all fields");
                    return;
                }

                boolean success = authService.createUser(username, password, "ADMIN");
                if (success) {
                    showSuccess("Admin user created successfully");
                } else {
                    showError("Failed to create admin user");
                }
            } catch (Exception e) {
                showError("Failed to create admin: " + e.getMessage());
            }
        }
    }

    private void showAddCourseDialog() {
        JTextField codeField = new JTextField(10);
        JTextField titleField = new JTextField(20);
        JTextField creditsField = new JTextField(5);
        JTextArea descriptionArea = new JTextArea(3, 20);
        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
        panel.add(new JLabel("Course Code:"));
        panel.add(codeField);
        panel.add(new JLabel("Course Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Credits:"));
        panel.add(creditsField);
        panel.add(new JLabel("Description:"));
        panel.add(descriptionScroll);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Add New Course", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                String code = codeField.getText().trim();
                String title = titleField.getText().trim();
                int credits = Integer.parseInt(creditsField.getText().trim());
                String description = descriptionArea.getText().trim();

                if (code.isEmpty() || title.isEmpty()) {
                    showError("Please fill required fields");
                    return;
                }

                Course course = new Course();
                course.setCode(code);
                course.setTitle(title);
                course.setCredits(credits);
                course.setDescription(description);

                boolean success = adminService.addCourse(course);
                if (success) {
                    showSuccess("Course created successfully");
                    loadCourses(); // Refresh the table
                } else {
                    showError("Failed to create course");
                }
            } catch (NumberFormatException e) {
                showError("Please enter valid credits");
            } catch (Exception e) {
                showError("Failed to create course: " + e.getMessage());
            }
        }
    }

    private void showAddSectionDialog() {
        try {
            List<Course> courses = adminService.getAvailableCourses();
            List<Instructor> instructors = adminService.getAvailableInstructors();

            if (courses.isEmpty() || instructors.isEmpty()) {
                showError("No courses or instructors available. Please add courses and instructors first.");
                return;
            }

            // Course selection
            JComboBox<String> courseCombo = new JComboBox<>();
            for (Course course : courses) {
                courseCombo.addItem(course.getCode() + " - " + course.getTitle());
            }

            // Instructor selection
            JComboBox<String> instructorCombo = new JComboBox<>();
            for (Instructor instructor : instructors) {
                instructorCombo.addItem(instructor.getUsername() + " - " + instructor.getDepartment());
            }

            JTextField dayTimeField = new JTextField(15);
            JTextField roomField = new JTextField(10);
            JTextField capacityField = new JTextField(5);
            JTextField semesterField = new JTextField(10);
            JTextField yearField = new JTextField(5);

            JPanel panel = new JPanel(new GridLayout(7, 2, 10, 10));
            panel.add(new JLabel("Course:"));
            panel.add(courseCombo);
            panel.add(new JLabel("Instructor:"));
            panel.add(instructorCombo);
            panel.add(new JLabel("Day/Time:"));
            panel.add(dayTimeField);
            panel.add(new JLabel("Room:"));
            panel.add(roomField);
            panel.add(new JLabel("Capacity:"));
            panel.add(capacityField);
            panel.add(new JLabel("Semester:"));
            panel.add(semesterField);
            panel.add(new JLabel("Year:"));
            panel.add(yearField);

            int result = JOptionPane.showConfirmDialog(this, panel,
                    "Add New Section", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                int selectedCourseIndex = courseCombo.getSelectedIndex();
                int selectedInstructorIndex = instructorCombo.getSelectedIndex();

                if (selectedCourseIndex == -1 || selectedInstructorIndex == -1) {
                    showError("Please select both course and instructor");
                    return;
                }

                int courseId = courses.get(selectedCourseIndex).getCourseId();
                int instructorId = instructors.get(selectedInstructorIndex).getUserId();
                String dayTime = dayTimeField.getText().trim();
                String room = roomField.getText().trim();
                int capacity = Integer.parseInt(capacityField.getText().trim());
                String semester = semesterField.getText().trim();
                int year = Integer.parseInt(yearField.getText().trim());

                if (dayTime.isEmpty() || room.isEmpty() || semester.isEmpty()) {
                    showError("Please fill all fields");
                    return;
                }

                boolean success = adminService.createSection(courseId, instructorId, dayTime, room, capacity, semester, year);
                if (success) {
                    showSuccess("Section created successfully");
                    loadSections(); // Refresh sections table
                } else {
                    showError("Failed to create section");
                }
            }
        } catch (NumberFormatException e) {
            showError("Please enter valid numbers for capacity and year");
        } catch (Exception e) {
            showError("Failed to create section: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAllSections() {
        try {
            List<Section> sections = adminService.getAllSections();

            JDialog dialog = new JDialog(this, "All Sections", true);
            dialog.setSize(800, 400);
            dialog.setLocationRelativeTo(this);

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel titleLabel = new JLabel("All Sections (" + sections.size() + " sections)");
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            panel.add(titleLabel, BorderLayout.NORTH);

            // Create table
            String[] columns = {"Section ID", "Course", "Instructor", "Day/Time", "Room", "Capacity", "Enrolled", "Semester", "Year"};
            DefaultTableModel model = new DefaultTableModel(columns, 0);

            for (Section section : sections) {
                model.addRow(new Object[]{
                        section.getSectionId(),
                        section.getCourseCode() + " - " + section.getCourseTitle(),
                        section.getInstructorName(),
                        section.getDayTime(),
                        section.getRoom(),
                        section.getCapacity(),
                        section.getEnrolledCount(),
                        section.getSemester(),
                        section.getYear()
                });
            }

            JTable sectionsTable = new JTable(model);
            JScrollPane scrollPane = new JScrollPane(sectionsTable);
            panel.add(scrollPane, BorderLayout.CENTER);

            JButton closeBtn = new JButton("Close");
            closeBtn.addActionListener(e -> dialog.dispose());

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(closeBtn);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            dialog.add(panel);
            dialog.setVisible(true);

        } catch (Exception e) {
            showError("Failed to load sections: " + e.getMessage());
        }
    }

    private void showAllUsers() {
        try {
            List<User> users = adminService.getAllUsers();

            JDialog dialog = new JDialog(this, "All System Users", true);
            dialog.setSize(600, 400);
            dialog.setLocationRelativeTo(this);

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel titleLabel = new JLabel("All Registered Users (" + users.size() + " users)");
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            panel.add(titleLabel, BorderLayout.NORTH);

            // Create table
            String[] columns = {"User ID", "Username", "Role", "Status"};
            DefaultTableModel model = new DefaultTableModel(columns, 0);

            for (User user : users) {
                model.addRow(new Object[]{
                        user.getUserId(),
                        user.getUsername(),
                        user.getRole(),
                        user.getStatus()
                });
            }

            JTable usersTable = new JTable(model);
            JScrollPane scrollPane = new JScrollPane(usersTable);
            panel.add(scrollPane, BorderLayout.CENTER);

            JButton closeBtn = new JButton("Close");
            closeBtn.addActionListener(e -> dialog.dispose());

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(closeBtn);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            dialog.add(panel);
            dialog.setVisible(true);

        } catch (Exception e) {
            showError("Failed to load users: " + e.getMessage());
        }
    }

    private void showStudents() {
        try {
            List<Student> students = adminService.getAllStudents();

            JDialog dialog = new JDialog(this, "All Students", true);
            dialog.setSize(700, 400);
            dialog.setLocationRelativeTo(this);

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel titleLabel = new JLabel("All Students (" + students.size() + " students)");
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            panel.add(titleLabel, BorderLayout.NORTH);

            // Create table
            String[] columns = {"Student ID", "Username", "Roll Number", "Program", "Year"};
            DefaultTableModel model = new DefaultTableModel(columns, 0);

            for (Student student : students) {
                model.addRow(new Object[]{
                        student.getUserId(),
                        student.getUsername(),
                        student.getRollNo(),
                        student.getProgram(),
                        student.getYear()
                });
            }

            JTable studentsTable = new JTable(model);
            JScrollPane scrollPane = new JScrollPane(studentsTable);
            panel.add(scrollPane, BorderLayout.CENTER);

            JButton closeBtn = new JButton("Close");
            closeBtn.addActionListener(e -> dialog.dispose());

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(closeBtn);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            dialog.add(panel);
            dialog.setVisible(true);

        } catch (Exception e) {
            showError("Failed to load students: " + e.getMessage());
        }
    }

    private void showInstructors() {
        try {
            List<Instructor> instructors = adminService.getAllInstructors();

            JDialog dialog = new JDialog(this, "All Instructors", true);
            dialog.setSize(600, 400);
            dialog.setLocationRelativeTo(this);

            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JLabel titleLabel = new JLabel("All Instructors (" + instructors.size() + " instructors)");
            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            panel.add(titleLabel, BorderLayout.NORTH);

            // Create table
            String[] columns = {"Instructor ID", "Username", "Department", "Office"};
            DefaultTableModel model = new DefaultTableModel(columns, 0);

            for (Instructor instructor : instructors) {
                model.addRow(new Object[]{
                        instructor.getUserId(),
                        instructor.getUsername(),
                        instructor.getDepartment(),
                        instructor.getOffice()
                });
            }

            JTable instructorsTable = new JTable(model);
            JScrollPane scrollPane = new JScrollPane(instructorsTable);
            panel.add(scrollPane, BorderLayout.CENTER);

            JButton closeBtn = new JButton("Close");
            closeBtn.addActionListener(e -> dialog.dispose());

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(closeBtn);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            dialog.add(panel);
            dialog.setVisible(true);

        } catch (Exception e) {
            showError("Failed to load instructors: " + e.getMessage());
        }
    }

    private void toggleMaintenanceMode() {
        try {
            boolean newStatus = maintenanceCheckbox.isSelected();
            boolean success = maintenanceService.toggleMaintenanceMode(newStatus);
            if (success) {
                showSuccess("Maintenance mode " + (newStatus ? "enabled" : "disabled"));
                // Refresh the panel to show/hide maintenance banner
                dispose();
                new AdminPanel(currentUser).setVisible(true);
            } else {
                showError("Failed to toggle maintenance mode");
            }
        } catch (Exception e) {
            showError("Failed to toggle maintenance mode: " + e.getMessage());
        }
    }

    private void backupDatabase() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select Backup Location");
            fileChooser.setSelectedFile(new java.io.File("university_erp_backup.sql"));

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                String backupPath = fileChooser.getSelectedFile().getAbsolutePath();
                boolean success = adminService.backupDatabase(backupPath);
                if (success) {
                    showSuccess("Database backup created successfully at: " + backupPath);
                } else {
                    showError("Failed to create backup");
                }
            }
        } catch (Exception e) {
            showError("Failed to backup database: " + e.getMessage());
        }
    }

    private void restoreDatabase() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select Backup File to Restore");

            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                String backupPath = fileChooser.getSelectedFile().getAbsolutePath();

                int confirm = JOptionPane.showConfirmDialog(this,
                        "WARNING: This will overwrite current data. Continue?",
                        "Confirm Restore", JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    boolean success = adminService.restoreDatabase(backupPath);
                    if (success) {
                        showSuccess("Database restored successfully from: " + backupPath);
                    } else {
                        showError("Failed to restore database");
                    }
                }
            }
        } catch (Exception e) {
            showError("Failed to restore database: " + e.getMessage());
        }
    }

    private String getTotalUsersCount() {
        try {
            List<User> users = adminService.getAllUsers();
            return String.valueOf(users.size());
        } catch (Exception e) {
            return "Unknown";
        }
    }
}