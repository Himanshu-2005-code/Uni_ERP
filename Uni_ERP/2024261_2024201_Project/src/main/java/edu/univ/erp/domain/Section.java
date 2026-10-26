package edu.univ.erp.domain;

/** A scheduled offering of a course, joined with its course and instructor names. */
public record Section(int id, int courseId, String courseCode, String courseTitle, int credits,
                      Integer instructorId, String instructorName, String dayTime, String room,
                      int capacity, int enrolledCount, String semester, int year, String department) {

    public int seatsLeft() {
        return Math.max(0, capacity - enrolledCount);
    }

    public boolean isFull() {
        return enrolledCount >= capacity;
    }

    public double fillRatio() {
        return capacity <= 0 ? 1 : Math.min(1.0, enrolledCount / (double) capacity);
    }

    public String term() {
        return semester + " " + year;
    }

    public String label() {
        return courseCode + " · " + courseTitle + " (" + dayTime + ")";
    }

    @Override
    public String toString() {
        return label();
    }
}
