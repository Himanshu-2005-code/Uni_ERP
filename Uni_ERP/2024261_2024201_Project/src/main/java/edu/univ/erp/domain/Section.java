package edu.univ.erp.domain;

public class Section {
    private int sectionId;
    private int courseId;
    private String courseCode;
    private String courseTitle;
    private int instructorId;
    private String instructorName;
    private String dayTime;
    private String room;
    private int capacity;
    private int enrolledCount;
    private String semester;
    private int year;

    public Section(int sectionId, int courseId, String courseCode, String courseTitle,
                   int instructorId, String instructorName, String dayTime, String room,
                   int capacity, int enrolledCount, String semester, int year) {
        this.sectionId = sectionId;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseTitle = courseTitle;
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.dayTime = dayTime;
        this.room = room;
        this.capacity = capacity;
        this.enrolledCount = enrolledCount;
        this.semester = semester;
        this.year = year;
    }

    // Getters and setters
    public int getSectionId() { return sectionId; }
    public int getCourseId() { return courseId; }
    public String getCourseCode() { return courseCode; }
    public String getCourseTitle() { return courseTitle; }
    public int getInstructorId() { return instructorId; }
    public String getInstructorName() { return instructorName; }
    public String getDayTime() { return dayTime; }
    public String getRoom() { return room; }
    public int getCapacity() { return capacity; }
    public int getEnrolledCount() { return enrolledCount; }
    public String getSemester() { return semester; }
    public int getYear() { return year; }

    public boolean hasSeatsAvailable() {
        return enrolledCount < capacity;
    }

    public int getAvailableSeats() {
        return capacity - enrolledCount;
    }

    @Override
    public String toString() {
        return String.format("%s - %s (%s) - %s", courseCode, courseTitle, dayTime, instructorName);
    }
}