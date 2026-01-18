package edu.univ.erp.service;

import edu.univ.erp.data.AdminDAO;
import edu.univ.erp.domain.User;
import edu.univ.erp.domain.Student;
import edu.univ.erp.domain.Instructor;
import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.Section;
import java.util.List;

public class AdminService {
    private AdminDAO adminDAO;

    public AdminService() {
        this.adminDAO = new AdminDAO();
    }

    // User Management
    public List<User> getAllUsers() {
        try {
            return adminDAO.getAllUsers();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get users: " + e.getMessage());
        }
    }

    public List<Student> getAllStudents() {
        try {
            return adminDAO.getAllStudents();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get students: " + e.getMessage());
        }
    }

    public List<Instructor> getAllInstructors() {
        try {
            return adminDAO.getAllInstructors();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get instructors: " + e.getMessage());
        }
    }

    // Course Management
    public List<Course> getAllCourses() {
        try {
            return adminDAO.getAllCourses();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get courses: " + e.getMessage());
        }
    }

    public boolean createCourse(String code, String title, int credits, String description) {
        try {
            return adminDAO.createCourse(code, title, credits, description);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create course: " + e.getMessage());
        }
    }

    public boolean addCourse(Course course) {
        try {
            return adminDAO.createCourse(course.getCode(), course.getTitle(),
                    course.getCredits(), course.getDescription());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create course: " + e.getMessage());
        }
    }

    public boolean deleteCourse(int courseId) {
        try {
            return adminDAO.deleteCourse(courseId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete course: " + e.getMessage());
        }
    }

    public boolean updateCourse(int courseId, String code, String title, int credits, String description) {
        try {
            return adminDAO.updateCourse(courseId, code, title, credits, description);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update course: " + e.getMessage());
        }
    }

    // Section Management
    public List<Section> getAllSections() {
        try {
            return adminDAO.getAllSections();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get sections: " + e.getMessage());
        }
    }

    public boolean createSection(int courseId, int instructorId, String dayTime,
                                 String room, int capacity, String semester, int year) {
        try {
            return adminDAO.createSection(courseId, instructorId, dayTime, room, capacity, semester, year);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create section: " + e.getMessage());
        }
    }

    public List<Course> getAvailableCourses() {
        try {
            return adminDAO.getAvailableCourses();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get available courses: " + e.getMessage());
        }
    }

    public List<Instructor> getAvailableInstructors() {
        try {
            return adminDAO.getAvailableInstructors();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get available instructors: " + e.getMessage());
        }
    }

    // Backup/Restore
    public boolean backupDatabase(String backupPath) {
        try {
            return adminDAO.backupDatabase(backupPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to backup database: " + e.getMessage());
        }
    }

    public boolean restoreDatabase(String backupPath) {
        try {
            return adminDAO.restoreDatabase(backupPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to restore database: " + e.getMessage());
        }
    }
}