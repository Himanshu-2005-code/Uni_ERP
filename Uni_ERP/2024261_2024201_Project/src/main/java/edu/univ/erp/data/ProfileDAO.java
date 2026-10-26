package edu.univ.erp.data;

import edu.univ.erp.db.Sql;
import edu.univ.erp.domain.InstructorProfile;
import edu.univ.erp.domain.StudentProfile;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Student and instructor profile rows, joined with their login account. */
public final class ProfileDAO {

    private static final String STUDENT_SQL = "SELECT u.user_id, u.username, u.full_name, u.email, u.status, "
            + "s.roll_no, s.program, s.year FROM students s JOIN university_auth.users_auth u ON u.user_id = s.user_id";
    private static final String INSTRUCTOR_SQL = "SELECT u.user_id, u.username, u.full_name, u.email, u.status, "
            + "i.department, i.office FROM instructors i JOIN university_auth.users_auth u ON u.user_id = i.user_id";

    private ProfileDAO() {
    }

    private static StudentProfile student(ResultSet rs) throws SQLException {
        return new StudentProfile(rs.getInt("user_id"), rs.getString("username"), rs.getString("full_name"),
                rs.getString("email"), rs.getString("status"), rs.getString("roll_no"), rs.getString("program"),
                rs.getInt("year"));
    }

    private static InstructorProfile instructor(ResultSet rs) throws SQLException {
        return new InstructorProfile(rs.getInt("user_id"), rs.getString("username"), rs.getString("full_name"),
                rs.getString("email"), rs.getString("status"), rs.getString("department"), rs.getString("office"));
    }

    public static Optional<StudentProfile> student(Connection c, int id) throws SQLException {
        return Sql.one(c, STUDENT_SQL + " WHERE s.user_id = ?", ProfileDAO::student, id);
    }

    public static List<StudentProfile> students(Connection c) throws SQLException {
        return Sql.list(c, STUDENT_SQL + " ORDER BY s.roll_no", ProfileDAO::student);
    }

    public static Optional<InstructorProfile> instructor(Connection c, int id) throws SQLException {
        return Sql.one(c, INSTRUCTOR_SQL + " WHERE i.user_id = ?", ProfileDAO::instructor, id);
    }

    public static List<InstructorProfile> instructors(Connection c) throws SQLException {
        return Sql.list(c, INSTRUCTOR_SQL + " ORDER BY i.department, u.full_name, u.username",
                ProfileDAO::instructor);
    }

    public static boolean rollExists(Connection c, String rollNo, int exceptUserId) throws SQLException {
        return Sql.count(c, "SELECT COUNT(*) FROM students WHERE roll_no = ? AND user_id <> ?",
                rollNo, exceptUserId) > 0;
    }

    public static void insertStudent(Connection c, int id, String rollNo, String program, int year)
            throws SQLException {
        Sql.update(c, "INSERT INTO students (user_id, roll_no, program, year) VALUES (?, ?, ?, ?)",
                id, rollNo, program, year);
    }

    public static void updateStudent(Connection c, int id, String rollNo, String program, int year)
            throws SQLException {
        Sql.update(c, "UPDATE students SET roll_no = ?, program = ?, year = ? WHERE user_id = ?",
                rollNo, program, year, id);
    }

    public static void insertInstructor(Connection c, int id, String department, String office)
            throws SQLException {
        Sql.update(c, "INSERT INTO instructors (user_id, department, office) VALUES (?, ?, ?)",
                id, department, office);
    }

    public static void updateInstructor(Connection c, int id, String department, String office)
            throws SQLException {
        Sql.update(c, "UPDATE instructors SET department = ?, office = ? WHERE user_id = ?",
                department, office, id);
    }

    public static void deleteStudent(Connection c, int id) throws SQLException {
        Sql.update(c, "DELETE FROM students WHERE user_id = ?", id);
    }

    public static void deleteInstructor(Connection c, int id) throws SQLException {
        Sql.update(c, "DELETE FROM instructors WHERE user_id = ?", id);
    }
}
