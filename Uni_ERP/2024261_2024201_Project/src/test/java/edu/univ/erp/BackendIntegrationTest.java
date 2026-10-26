package edu.univ.erp;

import edu.univ.erp.db.Database;
import edu.univ.erp.db.DemoSeeder;
import edu.univ.erp.db.SchemaManager;
import edu.univ.erp.db.Sql;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.GradeComponent;
import edu.univ.erp.domain.Role;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.User;
import edu.univ.erp.service.AdminService;
import edu.univ.erp.service.AuthService;
import edu.univ.erp.service.BackupService;
import edu.univ.erp.service.ExportService;
import edu.univ.erp.service.InstructorService;
import edu.univ.erp.service.ServiceException;
import edu.univ.erp.service.SettingsService;
import edu.univ.erp.service.StudentService;
import edu.univ.erp.util.TimeSlot;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end checks against a real MySQL/MariaDB server. They DROP and recreate the
 * university_auth and university_erp databases, so only run them on a test server:
 * mvn test -Derp.it=true
 */
@EnabledIfSystemProperty(named = "erp.it", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BackendIntegrationTest {

    private static final String PW = DemoSeeder.DEMO_PASSWORD;

    @BeforeAll
    static void freshCampus() throws Exception {
        try (var c = Database.serverConnection(); Statement s = c.createStatement()) {
            s.execute("DROP DATABASE IF EXISTS university_erp");
            s.execute("DROP DATABASE IF EXISTS university_auth");
        }
        Database.stop();
        SchemaManager.ensureSchema();
        assertTrue(SchemaManager.isEmpty());
        DemoSeeder.seed();
        SchemaManager.ensureSchema(); // must be idempotent on a populated database
    }

    private static User as(String username) {
        AuthService.logout();
        return AuthService.login(username, PW);
    }

    @Test
    @Order(1)
    void seededCountsAreConsistent() {
        int drift = Database.read(c -> Sql.count(c, "SELECT COUNT(*) FROM sections s WHERE enrolled_count <> "
                + "(SELECT COUNT(*) FROM enrollments e WHERE e.section_id = s.section_id AND e.status = 'REGISTERED')"));
        assertEquals(0, drift);
        assertEquals(Role.STUDENT, as("stu1").role());
        assertFalse(StudentService.catalog().isEmpty());
    }

    @Test
    @Order(2)
    void wrongPasswordLocksAccountAtThreshold() {
        AuthService.logout();
        int threshold = SettingsService.get().refresh().lockoutThreshold();
        for (int i = 1; i < threshold; i++) {
            ServiceException e = assertThrows(ServiceException.class, () -> AuthService.login("stu60", "nope"));
            assertTrue(e.getMessage().contains("left"), e.getMessage());
        }
        assertThrows(ServiceException.class, () -> AuthService.login("stu60", "nope"));
        ServiceException locked = assertThrows(ServiceException.class, () -> AuthService.login("stu60", PW));
        assertTrue(locked.getMessage().contains("locked"));
        as("admin1");
        int id = AdminService.users().stream().filter(u -> u.username().equals("stu60")).findFirst().orElseThrow().id();
        AdminService.setLocked(id, false);
        assertEquals("stu60", as("stu60").username());
    }

    @Test
    @Order(3)
    void dropThenReRegisterWorks() {
        as("stu2");
        Enrollment e = StudentService.currentRegistrations().get(0);
        int before = section(e.sectionId()).enrolledCount();
        StudentService.drop(e.id());
        assertEquals(before - 1, section(e.sectionId()).enrolledCount());
        // v1 failed here with a duplicate-key error
        Enrollment again = StudentService.register(e.sectionId());
        assertTrue(again.isActive());
        assertEquals(before, section(e.sectionId()).enrolledCount());
        assertTrue(StudentService.results().stream().noneMatch(r -> r.enrollment().id() == again.id()
                && !r.scores().isEmpty()), "old marks must be cleared on re-registration");
    }

    @Test
    @Order(4)
    void registrationRulesAreEnforced() {
        as("stu3");
        List<Enrollment> mine = StudentService.currentRegistrations();
        Section taken = mine.get(0).section();
        List<Section> catalog = StudentService.catalog();

        assertThrows(ServiceException.class, () -> StudentService.register(taken.id()));
        catalog.stream().filter(s -> s.courseId() == taken.courseId() && s.id() != taken.id()).findFirst()
                .ifPresent(other -> assertTrue(assertThrows(ServiceException.class,
                        () -> StudentService.register(other.id())).getMessage().contains("another section")));
        Section clash = catalog.stream()
                .filter(s -> mine.stream().noneMatch(m -> m.section().courseId() == s.courseId()))
                .filter(s -> mine.stream().anyMatch(m -> TimeSlot.clash(m.section().dayTime(), s.dayTime())))
                .findFirst().orElseThrow();
        assertTrue(assertThrows(ServiceException.class, () -> StudentService.register(clash.id()))
                .getMessage().contains("clash"));
    }

    @Test
    @Order(5)
    void fullSectionRejectsAndCannotDropSomeoneElses() {
        as("admin1");
        Section des = AdminService.sections().stream()
                .filter(s -> s.courseCode().equals("DES201") && s.term().equals(SettingsService.get().term()))
                .findFirst().orElseThrow();
        AdminService.saveSection(des.id(), new AdminService.SectionForm(des.courseId(), des.instructorId(),
                des.dayTime(), des.room(), Math.max(1, des.enrolledCount()), des.semester(), des.year()));
        String outsider = null;
        for (int n = 4; n <= 59 && outsider == null; n++) {
            as("stu" + n);
            boolean free = StudentService.currentRegistrations().stream().noneMatch(e ->
                    e.section().courseId() == des.courseId() || TimeSlot.clash(e.section().dayTime(), des.dayTime()));
            if (free && StudentService.currentCredits() <= 20) outsider = "stu" + n;
        }
        assertNotNull(outsider);
        as(outsider);
        assertTrue(assertThrows(ServiceException.class, () -> StudentService.register(des.id()))
                .getMessage().contains("full"));
        int othersEnrollment = Database.read(c -> Sql.count(c,
                "SELECT enrollment_id FROM enrollments WHERE section_id = ? AND status = 'REGISTERED' LIMIT 1", des.id()));
        assertTrue(assertThrows(ServiceException.class, () -> StudentService.drop(othersEnrollment))
                .getMessage().contains("isn't yours"));
    }

    @Test
    @Order(6)
    void gradebookValidatesAndFinalsStayFresh() {
        as("inst1");
        Section sec = InstructorService.sections().stream()
                .filter(s -> s.term().equals(SettingsService.get().term()) && s.enrolledCount() > 0).findFirst().orElseThrow();
        InstructorService.Gradebook gb = InstructorService.gradebook(sec.id());
        int eid = gb.roster().get(0).enrollmentId();

        Map<Integer, Map<String, Double>> bad = Map.of(eid, Map.of("Quiz", 999.0));
        assertThrows(ServiceException.class, () -> InstructorService.saveScores(sec.id(), bad));

        List<GradeComponent> wrongWeights = List.of(new GradeComponent(0, sec.id(), "Quiz", 20, 20, 1),
                new GradeComponent(0, sec.id(), "Final", 50, 50, 2));
        assertTrue(assertThrows(ServiceException.class, () -> InstructorService.saveComponents(sec.id(), wrongWeights))
                .getMessage().contains("100"));

        Map<Integer, Map<String, Double>> all = new HashMap<>();
        for (var r : gb.roster()) all.put(r.enrollmentId(), Map.of("Quiz", 18.0, "Midterm", 27.0, "Final", 45.0));
        InstructorService.saveScores(sec.id(), all);
        InstructorService.FinalizeResult res = InstructorService.computeFinals(sec.id());
        assertEquals(gb.roster().size(), res.computed());
        assertEquals("A+", InstructorService.gradebook(sec.id()).roster().get(0).finalGrade());

        InstructorService.saveScores(sec.id(), Map.of(eid, Map.of("Final", 10.0)));
        var row = InstructorService.gradebook(sec.id()).roster().stream().filter(r -> r.enrollmentId() == eid)
                .findFirst().orElseThrow();
        assertEquals(55.0, row.finalPercentage(), 0.01);
        assertEquals("B-", row.finalGrade());

        Map<String, Double> clear = new HashMap<>();
        clear.put("Final", null);
        InstructorService.saveScores(sec.id(), Map.of(eid, clear));
        assertNull(InstructorService.gradebook(sec.id()).roster().stream().filter(r -> r.enrollmentId() == eid)
                .findFirst().orElseThrow().finalGrade(), "unpublish when a component is cleared");

        as("inst2");
        assertThrows(ServiceException.class, () -> InstructorService.gradebook(sec.id()));
    }

    @Test
    @Order(7)
    void userCreationIsAtomicAndDeletionGuarded() {
        as("admin1");
        String existingRoll = AdminService.students().get(0).rollNo();
        var dup = new AdminService.UserForm("newkid", "secret123", "New Kid", null, Role.STUDENT, existingRoll,
                "B.Tech CSE", 2026, null, null);
        assertThrows(ServiceException.class, () -> AdminService.createUser(dup));
        assertTrue(AdminService.users().stream().noneMatch(u -> u.username().equals("newkid")), "no orphan login");

        int id = AdminService.createUser(new AdminService.UserForm("newkid", "secret123", "New Kid", "kid@x.edu",
                Role.STUDENT, "2026999", "B.Tech CSE", 2026, null, null));
        assertEquals("New Kid", AdminService.users().stream().filter(u -> u.id() == id).findFirst().orElseThrow().fullName());
        AdminService.deleteUser(id);

        int inst = AdminService.instructors().stream().filter(i -> i.username().equals("inst1")).findFirst()
                .orElseThrow().userId();
        assertTrue(assertThrows(ServiceException.class, () -> AdminService.deleteUser(inst)).getMessage()
                .contains("Reassign"));
        assertThrows(ServiceException.class, () -> AdminService.createUser(new AdminService.UserForm("weak", "short",
                null, null, Role.ADMIN, null, null, null, null, null)));
    }

    @Test
    @Order(8)
    void sectionSchedulingDetectsClashes() {
        as("admin1");
        Section any = AdminService.sections().stream().filter(s -> s.term().equals(SettingsService.get().term()))
                .findFirst().orElseThrow();
        var sameRoom = new AdminService.SectionForm(any.courseId(), null, any.dayTime(), any.room(), 30,
                any.semester(), any.year());
        assertTrue(assertThrows(ServiceException.class, () -> AdminService.saveSection(null, sameRoom))
                .getMessage().contains("booked"));
        var badSlot = new AdminService.SectionForm(any.courseId(), null, "sometime", "X-1", 30, any.semester(), any.year());
        assertThrows(ServiceException.class, () -> AdminService.saveSection(null, badSlot));
        int sid = AdminService.saveSection(null, new AdminService.SectionForm(any.courseId(), null,
                "Sat 08:00-09:00", "Z-999", 30, any.semester(), any.year()));
        AdminService.deleteSection(sid);
    }

    @Test
    @Order(9)
    void maintenanceBlocksStudentChanges() {
        as("admin1");
        SettingsService.get().setMaintenance(true);
        as("stu5");
        int eid = StudentService.currentRegistrations().get(0).id();
        assertTrue(assertThrows(ServiceException.class, () -> StudentService.drop(eid)).getMessage()
                .contains("maintenance"));
        as("admin1");
        SettingsService.get().setMaintenance(false);
    }

    @Test
    @Order(10)
    void transcriptAndDashboardRender() throws Exception {
        as("stu7");
        Path pdf = Files.createTempFile("transcript", ".pdf");
        ExportService.transcriptPdf(pdf);
        assertTrue(new String(Files.readAllBytes(pdf), 0, 8).startsWith("%PDF-1.4"));
        assertTrue(StudentService.gpa().cgpa() > 0);
        Path csv = Files.createTempFile("report", ".csv");
        ExportService.gradeReportCsv(csv);
        assertTrue(Files.size(csv) > 100);
        as("admin1");
        AdminService.Dashboard d = AdminService.dashboard();
        assertTrue(d.activeEnrollments() > 100);
        assertEquals(30, d.registrationTrend().size());
    }

    @Test
    @Order(11)
    void backupRestoreRoundTrip() throws Exception {
        as("admin1");
        int enrollments = Database.read(c -> Sql.count(c, "SELECT COUNT(*) FROM enrollments"));
        Path file = Files.createTempFile("erp", ".sql");
        BackupService.Summary s = BackupService.backup(file);
        assertTrue(s.rows() > 500);
        Database.txRun(c -> Sql.update(c, "DELETE FROM grades WHERE grade_id < 50"));
        Path safety = BackupService.restore(file);
        assertTrue(Files.exists(safety));
        int restored = Database.read(c -> Sql.count(c, "SELECT COUNT(*) FROM enrollments"));
        int early = Database.read(c -> Sql.count(c, "SELECT COUNT(*) FROM grades WHERE grade_id < 50"));
        assertEquals(enrollments, restored);
        assertEquals(49, early);
        assertEquals("admin1", as("admin1").username());
    }

    private static Section section(int id) {
        return Database.read(c -> edu.univ.erp.data.SectionDAO.find(c, id)).orElseThrow();
    }
}
