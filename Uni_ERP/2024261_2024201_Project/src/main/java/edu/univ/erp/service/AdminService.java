package edu.univ.erp.service;

import edu.univ.erp.data.AnnouncementDAO;
import edu.univ.erp.data.AuditDAO;
import edu.univ.erp.data.CourseDAO;
import edu.univ.erp.data.EnrollmentDAO;
import edu.univ.erp.data.GradeDAO;
import edu.univ.erp.data.ProfileDAO;
import edu.univ.erp.data.SectionDAO;
import edu.univ.erp.data.StatsDAO;
import edu.univ.erp.data.UserDAO;
import edu.univ.erp.db.Database;
import edu.univ.erp.db.Sql;
import edu.univ.erp.domain.Announcement;
import edu.univ.erp.domain.AuditEntry;
import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.InstructorProfile;
import edu.univ.erp.domain.LabelValue;
import edu.univ.erp.domain.Role;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.StudentProfile;
import edu.univ.erp.domain.User;
import edu.univ.erp.util.TimeSlot;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Administration: accounts, catalogue, scheduling and reporting. */
public final class AdminService {

    public record Dashboard(Map<String, Integer> usersByRole, int courses, int sectionsThisTerm, int activeEnrollments,
                            double capacityUsed, List<LabelValue> enrollmentsByCourse, List<LabelValue> gradeDistribution,
                            List<LabelValue> studentsByProgram, List<LabelValue> registrationTrend,
                            List<Object[]> deptByProgram, List<AuditEntry> recentActivity) {
    }

    /** Fields for creating or editing an account; profile fields are used according to the role. */
    public record UserForm(String username, String password, String fullName, String email, Role role,
                           String rollNo, String program, Integer year, String department, String office) {
    }

    public record SectionForm(int courseId, Integer instructorId, String dayTime, String room, int capacity,
                              String semester, int year) {
    }

    private AdminService() {
    }

    private static void admin() {
        Session.require(Role.ADMIN);
    }

    // ---- Accounts -------------------------------------------------------------------------

    public static List<User> users() {
        admin();
        return Database.read(UserDAO::listAll);
    }

    public static List<StudentProfile> students() {
        admin();
        return Database.read(ProfileDAO::students);
    }

    public static List<InstructorProfile> instructors() {
        admin();
        return Database.read(ProfileDAO::instructors);
    }

    /** Creates the login and the profile in one transaction, so a failure leaves nothing behind. */
    public static int createUser(UserForm f) {
        admin();
        Validate.username(f.username());
        AuthService.checkPolicy(f.password());
        Validate.email(f.email());
        validateProfile(f);
        return Database.tx(c -> {
            if (UserDAO.usernameExists(c, f.username().trim())) {
                throw new ServiceException("The username " + f.username().trim() + " is taken.");
            }
            if (f.role() == Role.STUDENT && ProfileDAO.rollExists(c, f.rollNo().trim(), -1)) {
                throw new ServiceException("Roll number " + f.rollNo().trim() + " is already in use.");
            }
            int id = UserDAO.insert(c, f.username().trim(), f.role(), AuthService.hash(f.password()),
                    Validate.trimToNull(f.fullName()), Validate.trimToNull(f.email()));
            if (f.role() == Role.STUDENT) {
                ProfileDAO.insertStudent(c, id, f.rollNo().trim(), Validate.trimToNull(f.program()), f.year());
            } else if (f.role() == Role.INSTRUCTOR) {
                ProfileDAO.insertInstructor(c, id, Validate.trimToNull(f.department()), Validate.trimToNull(f.office()));
            }
            Audit.log(c, "USER_CREATED", f.role() + " " + f.username().trim());
            return id;
        });
    }

    public static void updateUser(int id, UserForm f) {
        admin();
        Validate.email(f.email());
        validateProfile(f);
        Database.txRun(c -> {
            User u = UserDAO.find(c, id).orElseThrow(() -> new ServiceException("User not found."));
            UserDAO.updateContact(c, id, Validate.trimToNull(f.fullName()), Validate.trimToNull(f.email()));
            if (u.role() == Role.STUDENT) {
                if (ProfileDAO.rollExists(c, f.rollNo().trim(), id)) {
                    throw new ServiceException("Roll number " + f.rollNo().trim() + " is already in use.");
                }
                ProfileDAO.updateStudent(c, id, f.rollNo().trim(), Validate.trimToNull(f.program()), f.year());
            } else if (u.role() == Role.INSTRUCTOR) {
                ProfileDAO.updateInstructor(c, id, Validate.trimToNull(f.department()), Validate.trimToNull(f.office()));
            }
            Audit.log(c, "USER_UPDATED", u.username());
        });
    }

    private static void validateProfile(UserForm f) {
        if (f.role() == Role.STUDENT) {
            Validate.required(f.rollNo(), "Roll number");
            if (f.year() == null) throw new ServiceException("Batch year is required.");
            Validate.range(f.year(), 1990, 2100, "Batch year");
        }
    }

    public static void setLocked(int id, boolean locked) {
        admin();
        if (id == Session.user().id()) throw new ServiceException("You can't lock your own account.");
        Database.txRun(c -> {
            User u = UserDAO.find(c, id).orElseThrow(() -> new ServiceException("User not found."));
            UserDAO.setStatus(c, id, locked ? "LOCKED" : "ACTIVE");
            Audit.log(c, locked ? "USER_LOCKED" : "USER_UNLOCKED", u.username());
        });
    }

    public static void resetPassword(int id, String password) {
        admin();
        AuthService.checkPolicy(password);
        Database.txRun(c -> {
            User u = UserDAO.find(c, id).orElseThrow(() -> new ServiceException("User not found."));
            UserDAO.setPassword(c, id, AuthService.hash(password));
            UserDAO.setStatus(c, id, "ACTIVE");
            Audit.log(c, "PASSWORD_RESET", u.username());
        });
    }

    /**
     * Deletes an account. Students lose their registrations and marks (the caller must
     * confirm that); instructors must have their sections reassigned first, and the last
     * administrator can't be removed.
     */
    public static void deleteUser(int id) {
        admin();
        if (id == Session.user().id()) throw new ServiceException("You can't delete the account you're signed in with.");
        Database.txRun(c -> {
            User u = UserDAO.find(c, id).orElseThrow(() -> new ServiceException("User not found."));
            switch (u.role()) {
                case STUDENT -> {
                    EnrollmentDAO.deleteForStudent(c, id);
                    ProfileDAO.deleteStudent(c, id);
                    Sql.update(c, "UPDATE sections s SET enrolled_count = (SELECT COUNT(*) FROM enrollments e "
                            + "WHERE e.section_id = s.section_id AND e.status = 'REGISTERED')");
                }
                case INSTRUCTOR -> {
                    int n = SectionDAO.countForInstructor(c, id);
                    if (n > 0) {
                        throw new ServiceException(u.displayName() + " teaches " + n + " section(s). Reassign them first.");
                    }
                    ProfileDAO.deleteInstructor(c, id);
                }
                case ADMIN -> {
                    if (UserDAO.countByRole(c).get("ADMIN") <= 1) {
                        throw new ServiceException("You can't delete the last administrator.");
                    }
                }
            }
            Sql.update(c, "DELETE FROM announcements WHERE author_id = ?", id);
            UserDAO.delete(c, id);
            Audit.log(c, "USER_DELETED", u.role() + " " + u.username());
        });
    }

    public static int enrollmentCount(int studentId) {
        admin();
        return Database.read(c -> EnrollmentDAO.countForStudent(c, studentId));
    }

    // ---- Courses --------------------------------------------------------------------------

    public static List<Course> courses() {
        return Database.read(CourseDAO::listAll);
    }

    public static int saveCourse(Integer id, String code, String title, int credits, String description,
                                 String department) {
        admin();
        String cd = Validate.required(code, "Course code").toUpperCase();
        String t = Validate.required(title, "Title");
        if (cd.length() > 20) throw new ServiceException("Course code must be 20 characters or fewer.");
        if (t.length() > 200) throw new ServiceException("Title must be 200 characters or fewer.");
        Validate.range(credits, 1, 12, "Credits");
        return Database.tx(c -> {
            if (CourseDAO.codeExists(c, cd, id == null ? -1 : id)) {
                throw new ServiceException("Course code " + cd + " already exists.");
            }
            if (id == null) {
                int nid = CourseDAO.insert(c, cd, t, credits, Validate.trimToNull(description), Validate.trimToNull(department));
                Audit.log(c, "COURSE_CREATED", cd);
                return nid;
            }
            CourseDAO.update(c, id, cd, t, credits, Validate.trimToNull(description), Validate.trimToNull(department));
            Audit.log(c, "COURSE_UPDATED", cd);
            return id;
        });
    }

    public static void deleteCourse(int id) {
        admin();
        Database.txRun(c -> {
            Course course = CourseDAO.find(c, id).orElseThrow(() -> new ServiceException("Course not found."));
            int n = CourseDAO.sectionCount(c, id);
            if (n > 0) throw new ServiceException(course.code() + " has " + n + " section(s). Delete those first.");
            CourseDAO.delete(c, id);
            Audit.log(c, "COURSE_DELETED", course.code());
        });
    }

    // ---- Sections -------------------------------------------------------------------------

    public static List<Section> sections() {
        admin();
        return Database.read(SectionDAO::listAll);
    }

    public static int saveSection(Integer id, SectionForm f) {
        admin();
        if (!TimeSlot.isValid(f.dayTime())) {
            throw new ServiceException("Use a schedule like \"Mon Wed 10:00-11:30\".");
        }
        Validate.required(f.room(), "Room");
        Validate.required(f.semester(), "Semester");
        Validate.range(f.capacity(), 1, 2000, "Capacity");
        Validate.range(f.year(), 2000, 2100, "Year");
        return Database.tx(c -> {
            CourseDAO.find(c, f.courseId()).orElseThrow(() -> new ServiceException("Choose a course."));
            if (f.instructorId() != null && ProfileDAO.instructor(c, f.instructorId()).isEmpty()) {
                throw new ServiceException("That instructor no longer exists.");
            }
            checkClashes(c, id, f);
            String slot = TimeSlot.parse(f.dayTime()).toString();
            if (id == null) {
                int nid = SectionDAO.insert(c, f.courseId(), f.instructorId(), slot, f.room().trim(), f.capacity(),
                        f.semester().trim(), f.year());
                GradeDAO.addDefaultComponents(c, nid);
                Audit.log(c, "SECTION_CREATED", "#" + nid + " " + slot + " " + f.room().trim());
                return nid;
            }
            SectionDAO.lock(c, id);
            int enrolled = SectionDAO.recount(c, id);
            if (f.capacity() < enrolled) {
                throw new ServiceException("Capacity can't be below the " + enrolled + " students already registered.");
            }
            SectionDAO.update(c, id, f.courseId(), f.instructorId(), slot, f.room().trim(), f.capacity(),
                    f.semester().trim(), f.year());
            Audit.log(c, "SECTION_UPDATED", "#" + id + " " + slot + " " + f.room().trim());
            return id;
        });
    }

    private static void checkClashes(Connection c, Integer id, SectionForm f) throws SQLException {
        for (Section other : SectionDAO.listByTerm(c, f.semester().trim(), f.year())) {
            if (id != null && other.id() == id) continue;
            if (!TimeSlot.clash(other.dayTime(), f.dayTime())) continue;
            if (other.room() != null && other.room().equalsIgnoreCase(f.room().trim())) {
                throw new ServiceException("Room " + other.room() + " is booked then for " + other.courseCode()
                        + " (" + other.dayTime() + ").");
            }
            if (f.instructorId() != null && f.instructorId().equals(other.instructorId())) {
                throw new ServiceException(other.instructorName() + " already teaches " + other.courseCode()
                        + " at that time.");
            }
        }
    }

    public static void deleteSection(int id) {
        admin();
        Database.txRun(c -> {
            Section s = SectionDAO.find(c, id).orElseThrow(() -> new ServiceException("Section not found."));
            int n = SectionDAO.enrollmentCount(c, id);
            if (n > 0) throw new ServiceException("This section has " + n + " registration record(s) and can't be deleted.");
            SectionDAO.delete(c, id);
            Audit.log(c, "SECTION_DELETED", s.courseCode() + " #" + id);
        });
    }

    public static List<String> departments() {
        return Database.read(c -> {
            TreeSet<String> d = new TreeSet<>();
            for (Course course : CourseDAO.listAll(c)) if (course.department() != null) d.add(course.department());
            for (InstructorProfile i : ProfileDAO.instructors(c)) if (i.department() != null) d.add(i.department());
            return List.copyOf(d);
        });
    }

    // ---- Notices, audit, reporting --------------------------------------------------------

    public static List<Announcement> announcements(int limit) {
        admin();
        return Database.read(c -> AnnouncementDAO.all(c, limit));
    }

    public static void postAnnouncement(String title, String body) {
        admin();
        String t = Validate.required(title, "Title");
        String b = Validate.required(body, "Message");
        if (t.length() > 150) throw new ServiceException("Title must be 150 characters or fewer.");
        Database.txRun(c -> {
            AnnouncementDAO.insert(c, Session.user().id(), null, t, b);
            Audit.log(c, "ANNOUNCEMENT", "Campus: " + t);
        });
    }

    public static void deleteAnnouncement(int id) {
        admin();
        Database.txRun(c -> {
            AnnouncementDAO.delete(c, id);
            Audit.log(c, "ANNOUNCEMENT_DELETED", "#" + id);
        });
    }

    public static List<AuditEntry> audit(int limit) {
        admin();
        return Database.read(c -> AuditDAO.recent(c, limit));
    }

    public static Dashboard dashboard() {
        admin();
        SettingsService s = SettingsService.get().refresh();
        return Database.read(c -> new Dashboard(
                UserDAO.countByRole(c),
                StatsDAO.count(c, "courses"),
                StatsDAO.sectionsInTerm(c, s.semester(), s.year()),
                StatsDAO.activeEnrollments(c),
                StatsDAO.capacityUsed(c, s.semester(), s.year()),
                StatsDAO.enrollmentsByCourse(c, s.semester(), s.year(), 10),
                StatsDAO.gradeDistribution(c),
                StatsDAO.studentsByProgram(c),
                StatsDAO.registrationTrend(c, 30),
                StatsDAO.enrollmentsByDepartmentAndProgram(c),
                AuditDAO.recent(c, 8)));
    }
}
