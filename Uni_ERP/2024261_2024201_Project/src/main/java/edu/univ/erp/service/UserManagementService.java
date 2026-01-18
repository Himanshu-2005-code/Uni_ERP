package edu.univ.erp.service;

import edu.univ.erp.data.StudentDAO;
import edu.univ.erp.data.InstructorDAO;
import edu.univ.erp.data.AdminDAO;
import edu.univ.erp.service.AuthService;
import edu.univ.erp.domain.Student;
import edu.univ.erp.domain.Instructor;


public class UserManagementService {
    private StudentDAO studentDAO;
    private InstructorDAO instructorDAO;
    private AdminDAO adminDAO;
    private AuthService authService;


    public UserManagementService() {
        this.studentDAO = new StudentDAO();
        this.instructorDAO = new InstructorDAO();
        this.adminDAO = new AdminDAO();
        this.authService = new AuthService();
    }

    //Create a new student user with profile    
    public boolean createStudent(String username, String password, String rollNo, String program, int year) {
        try {
            // First create the user in auth database
            boolean authCreated = authService.createUser(username, password, "STUDENT");
            if (!authCreated) {
                System.err.println("Failed to create user in auth database");
                return false;
            }

            // Get the created user ID
            int userId = authService.getUserIdByUsername(username);
            if (userId == -1) {
                System.err.println("Could not find created user ID");
                return false;
            }

            // Create student profile in ERP database
            Student student = new Student(userId, username, "ACTIVE", rollNo, program, year);

            return studentDAO.addStudent(student);

        } catch (Exception e) {
            System.err.println("Error creating student: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    //Create a new instructor user with profile    
    public boolean createInstructor(String username, String password, String department, String office) {
        try {
            // First create the user in auth database
            boolean authCreated = authService.createUser(username, password, "INSTRUCTOR");
            if (!authCreated) {
                System.err.println("Failed to create user in auth database");
                return false;
            }

            // Get the created user ID
            int userId = authService.getUserIdByUsername(username);
            if (userId == -1) {
                System.err.println("Could not find created user ID");
                return false;
            }

            // Create instructor profile in ERP database
            Instructor instructor = new Instructor(userId, username, "ACTIVE", department, office);

            return instructorDAO.addInstructor(instructor);

        } catch (Exception e) {
            System.err.println("Error creating instructor: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}