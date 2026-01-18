package edu.univ.erp.domain;

public class Instructor extends User {
    private String department;
    private String office;


    public Instructor() {
        super(0, "", "INSTRUCTOR", "ACTIVE");
    }

    public Instructor(int userId, String username, String status, String department, String office) {
        super(userId, username, "INSTRUCTOR", status);
        this.department = department;
        this.office = office;
    }

    // Getters and setters
    public String getDepartment() { return department; }
    public String getOffice() { return office; }

    public void setDepartment(String department) { this.department = department; }
    public void setOffice(String office) { this.office = office; }

    @Override
    public String toString() {
        return String.format("Prof. %s (%s)", getUsername(), department);
    }
}