package edu.univ.erp.db;

import edu.univ.erp.data.AnnouncementDAO;
import edu.univ.erp.data.AuditDAO;
import edu.univ.erp.data.CourseDAO;
import edu.univ.erp.data.EnrollmentDAO;
import edu.univ.erp.data.GradeDAO;
import edu.univ.erp.data.ProfileDAO;
import edu.univ.erp.data.SectionDAO;
import edu.univ.erp.data.SettingsDAO;
import edu.univ.erp.data.UserDAO;
import edu.univ.erp.domain.GradeComponent;
import edu.univ.erp.domain.Role;
import edu.univ.erp.util.GradeScale;
import edu.univ.erp.util.TimeSlot;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Fills an empty database with a believable campus: 8 instructors, 60 students,
 * 17 courses and three terms of sections, registrations and marks.
 * Every demo account uses the password {@value #DEMO_PASSWORD}.
 */
public final class DemoSeeder {

    public static final String DEMO_PASSWORD = "password123";

    private static final String[] FIRST = {"Aarav", "Diya", "Vihaan", "Ananya", "Arjun", "Ishita", "Kabir", "Saanvi",
            "Reyansh", "Myra", "Aditya", "Kiara", "Rohan", "Tara", "Krish", "Anika", "Yash", "Riya", "Dev", "Meera",
            "Aryan", "Navya", "Ishaan", "Zara", "Laksh", "Pari", "Neel", "Avni", "Om", "Siya"};
    private static final String[] LAST = {"Sharma", "Verma", "Iyer", "Gupta", "Nair", "Reddy", "Kapoor", "Mehta",
            "Singh", "Das", "Bose", "Joshi", "Malhotra", "Chopra", "Rao", "Kulkarni", "Banerjee", "Agarwal", "Pillai",
            "Saxena"};
    private static final String[] PROGRAMS = {"B.Tech CSE", "B.Tech CSE", "B.Tech ECE", "B.Tech CSD", "B.Tech CSAM",
            "B.Tech CSSS"};

    private record Course(String code, String title, int credits, String dept, String description) {
    }

    private record Sec(String code, String instructor, String slot, String room, int capacity) {
    }

    private static final List<Course> COURSES = List.of(
            new Course("CSE101", "Introduction to Programming", 4, "Computer Science", "Programming fundamentals in Python: control flow, functions, data structures and testing."),
            new Course("CSE112", "Computer Organisation", 4, "Computer Science", "Number systems, assembly, datapaths, memory hierarchy and a simple simulator."),
            new Course("CSE201", "Data Structures", 4, "Computer Science", "Lists, trees, heaps, hashing and graphs with complexity analysis."),
            new Course("CSE202", "Database Management Systems", 4, "Computer Science", "Relational model, SQL, normalisation, indexing and transactions."),
            new Course("CSE222", "Algorithm Design and Analysis", 4, "Computer Science", "Greedy, divide and conquer, dynamic programming, flows and NP-completeness."),
            new Course("CSE231", "Operating Systems", 4, "Computer Science", "Processes, scheduling, memory virtualisation, concurrency and file systems."),
            new Course("CSE343", "Machine Learning", 4, "Computer Science", "Regression, classification, kernels, ensembles and neural networks."),
            new Course("MTH100", "Linear Algebra", 4, "Mathematics", "Vector spaces, linear maps, eigenvalues and applications."),
            new Course("MTH203", "Multivariate Calculus", 4, "Mathematics", "Differential and integral calculus of several variables."),
            new Course("ECE111", "Digital Circuits", 4, "Electronics", "Boolean algebra, combinational and sequential circuit design."),
            new Course("ECE250", "Signals and Systems", 4, "Electronics", "Continuous and discrete signals, transforms and LTI systems."),
            new Course("DES102", "Introduction to HCI", 4, "Design", "Design thinking, prototyping and usability evaluation."),
            new Course("DES201", "Visual Design", 4, "Design", "Typography, colour, layout and visual hierarchy."),
            new Course("COM101", "Communication Skills", 2, "Social Sciences", "Writing, presenting and professional communication."),
            new Course("SSH201", "Research Methods", 4, "Social Sciences", "Quantitative and qualitative research methods."),
            new Course("SG101", "Self Growth", 2, "Student Affairs", "Self-growth in a discipline such as chess, swimming or music."),
            new Course("CW101", "Community Work", 2, "Student Affairs", "Supervised community work with partner NGOs."));

    private static final String[][] INSTRUCTORS = {
            {"inst1", "Dr. Ananya Sharma", "Computer Science", "A-305"},
            {"inst2", "Dr. Rohit Verma", "Mathematics", "B-210"},
            {"inst3", "Prof. Meera Iyer", "Electronics", "C-118"},
            {"inst4", "Dr. Arjun Mehta", "Computer Science", "A-311"},
            {"inst5", "Dr. Kavya Nair", "Design", "D-102"},
            {"inst6", "Prof. Sanjay Gupta", "Social Sciences", "B-114"},
            {"inst7", "Dr. Priya Kapoor", "Mathematics", "B-216"},
            {"inst8", "Dr. Vikram Singh", "Electronics", "C-124"}};

    private static final List<Sec> CURRENT = List.of(
            new Sec("CSE101", "inst1", "Mon Wed 09:00-10:30", "C-101", 120),
            new Sec("CSE101", "inst4", "Tue Thu 09:00-10:30", "C-102", 120),
            new Sec("CSE112", "inst3", "Tue Thu 09:00-10:30", "LHC-101", 90),
            new Sec("CSE201", "inst4", "Mon Wed 11:00-12:30", "C-101", 90),
            new Sec("CSE202", "inst1", "Tue Thu 11:00-12:30", "C-201", 80),
            new Sec("CSE222", "inst1", "Mon Wed 14:00-15:30", "C-201", 70),
            new Sec("CSE231", "inst1", "Tue Thu 16:00-17:30", "C-101", 80),
            new Sec("CSE343", "inst4", "Tue Thu 14:00-15:30", "LHC-101", 100),
            new Sec("MTH100", "inst2", "Mon Wed 11:00-12:30", "LHC-201", 150),
            new Sec("MTH203", "inst7", "Tue Thu 11:00-12:30", "LHC-201", 120),
            new Sec("ECE111", "inst3", "Mon Wed 16:00-17:30", "LHC-101", 90),
            new Sec("ECE250", "inst8", "Tue Thu 16:00-17:30", "LHC-102", 80),
            new Sec("DES102", "inst5", "Mon Wed 14:00-15:30", "B-003", 60),
            new Sec("DES201", "inst5", "Tue Thu 14:00-15:30", "B-003", 12),
            new Sec("COM101", "inst6", "Mon Wed 09:00-10:30", "C-202", 50),
            new Sec("SSH201", "inst6", "Fri 10:00-13:00", "C-102", 60),
            new Sec("SG101", "inst6", "Fri 14:00-16:00", "Sports Complex", 200));

    private static final List<Sec> PAST = List.of(
            new Sec("CSE101", "inst1", "Mon Wed 09:00-10:30", "C-101", 150),
            new Sec("CSE112", "inst3", "Tue Thu 09:00-10:30", "LHC-101", 120),
            new Sec("CSE201", "inst4", "Mon Wed 11:00-12:30", "C-101", 120),
            new Sec("MTH100", "inst2", "Tue Thu 11:00-12:30", "LHC-201", 150),
            new Sec("ECE111", "inst8", "Mon Wed 16:00-17:30", "LHC-101", 120),
            new Sec("DES102", "inst5", "Mon Wed 14:00-15:30", "B-003", 100),
            new Sec("COM101", "inst6", "Fri 10:00-13:00", "C-102", 150),
            new Sec("CW101", "inst6", "Fri 14:00-16:00", "Community Hall", 200));

    private final Random rnd = new Random(2026);
    private final Map<String, Integer> users = new HashMap<>();
    private final Map<String, Integer> courseIds = new HashMap<>();
    private final Map<String, Integer> credits = new HashMap<>();

    private DemoSeeder() {
    }

    /** Loads the demo campus. The database must be empty. */
    public static void seed() {
        Database.txRun(c -> new DemoSeeder().run(c));
    }

    /** Creates just one administrator account (for a clean production start). */
    public static void createAdmin(String username, String password, String fullName) {
        Database.txRun(c -> {
            int id = UserDAO.insert(c, username, Role.ADMIN, BCrypt.hashpw(password, BCrypt.gensalt(10)), fullName, null);
            AuditDAO.log(c, id, username, "SETUP", "Initial administrator created");
        });
    }

    private void run(Connection c) throws SQLException {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        String hash = BCrypt.hashpw(DEMO_PASSWORD, BCrypt.gensalt(10));
        SettingsDAO.set(c, "institution_name", "Campus University");
        SettingsDAO.set(c, "current_semester", "Monsoon");
        SettingsDAO.set(c, "current_year", String.valueOf(year));
        SettingsDAO.set(c, "add_deadline", today.plusDays(21).toString());
        SettingsDAO.set(c, "drop_deadline", today.plusDays(45).toString());
        SettingsDAO.set(c, "maintenance_mode", "false");
        SettingsDAO.set(c, "demo_data", "true");

        users.put("admin1", UserDAO.insert(c, "admin1", Role.ADMIN, hash, "Neha Arora", "admin@campus.edu"));
        users.put("registrar", UserDAO.insert(c, "registrar", Role.ADMIN, hash, "Rahul Khanna", "registrar@campus.edu"));
        for (String[] i : INSTRUCTORS) {
            int id = UserDAO.insert(c, i[0], Role.INSTRUCTOR, hash, i[1],
                    i[0] + "@campus.edu");
            ProfileDAO.insertInstructor(c, id, i[2], i[3]);
            users.put(i[0], id);
        }
        List<Integer> students = new ArrayList<>();
        Map<Integer, Double> ability = new HashMap<>();
        Map<Integer, Integer> batch = new HashMap<>();
        for (int n = 1; n <= 60; n++) {
            String name = FIRST[(n * 7) % FIRST.length] + " " + LAST[(n * 11) % LAST.length];
            int b = n <= 20 ? year - 2 : n <= 45 ? year - 1 : year;
            String user = "stu" + n;
            int id = UserDAO.insert(c, user, Role.STUDENT, hash, name, user + "@campus.edu");
            ProfileDAO.insertStudent(c, id, String.format("%d%03d", b, n), PROGRAMS[n % PROGRAMS.length], b);
            users.put(user, id);
            students.add(id);
            ability.put(id, clamp(68 + rnd.nextGaussian() * 11, 38, 96));
            batch.put(id, b);
        }
        for (Course co : COURSES) {
            courseIds.put(co.code(), CourseDAO.insert(c, co.code(), co.title(), co.credits(), co.description(), co.dept()));
            credits.put(co.code(), co.credits());
        }

        // Two completed terms with published grades
        String[][] pastTerms = {{"Monsoon", String.valueOf(year - 1)}, {"Winter", String.valueOf(year)}};
        int termIndex = 0;
        for (String[] term : pastTerms) {
            List<Integer> secIds = new ArrayList<>();
            List<String> secSlots = new ArrayList<>();
            for (Sec s : PAST) {
                secIds.add(section(c, s, term[0], Integer.parseInt(term[1])));
                secSlots.add(s.slot());
            }
            for (int sid : students) {
                if (termIndex == 0 && batch.get(sid) == year) continue; // freshers joined this year
                List<Integer> picked = pick(secIds, secSlots, 4 + rnd.nextInt(2));
                for (int sec : picked) {
                    int eid = EnrollmentDAO.insert(c, sid, sec);
                    LocalDateTime when = LocalDateTime.of(Integer.parseInt(term[1]), termIndex == 0 ? 8 : 1, 5, 10, 0)
                            .plusDays(rnd.nextInt(10));
                    Sql.update(c, "UPDATE enrollments SET status = 'COMPLETED', enrollment_date = ? WHERE enrollment_id = ?",
                            when, eid);
                    double pct = markAll(c, eid, sec, ability.get(sid), true);
                    EnrollmentDAO.setFinal(c, eid, GradeScale.letter(pct), Math.round(pct * 10) / 10.0);
                }
            }
            for (int sec : secIds) SectionDAO.recount(c, sec);
            termIndex++;
        }

        // Current term: registrations spread over the last four weeks, quiz and midterm marked
        List<Integer> curIds = new ArrayList<>();
        List<String> curSlots = new ArrayList<>();
        for (Sec s : CURRENT) {
            curIds.add(section(c, s, "Monsoon", year));
            curSlots.add(s.slot());
        }
        Map<Integer, Integer> taken = new HashMap<>();
        for (int sid : students) {
            List<Integer> picked = pick(curIds, curSlots, 4 + rnd.nextInt(2));
            int cr = 0;
            for (int sec : picked) {
                int cap = CURRENT.get(curIds.indexOf(sec)).capacity();
                String code = CURRENT.get(curIds.indexOf(sec)).code();
                if (taken.getOrDefault(sec, 0) >= cap || cr + credits.get(code) > 24) continue;
                taken.merge(sec, 1, Integer::sum);
                cr += credits.get(code);
                int eid = EnrollmentDAO.insert(c, sid, sec);
                Sql.update(c, "UPDATE enrollments SET enrollment_date = ? WHERE enrollment_id = ?",
                        LocalDateTime.now().minusDays(rnd.nextInt(28)).minusMinutes(rnd.nextInt(600)), eid);
                markAll(c, eid, sec, ability.get(sid), false);
            }
        }
        for (int sec : curIds) SectionDAO.recount(c, sec);

        int admin = users.get("admin1");
        AnnouncementDAO.insert(c, admin, null, "Welcome to the " + "Monsoon " + year + " term",
                "Registration is open until " + today.plusDays(21) + ". Check your timetable for clashes before the add deadline.");
        AnnouncementDAO.insert(c, admin, null, "Library hours extended",
                "The central library is open until midnight on weekdays during the exam period.");
        AnnouncementDAO.insert(c, users.get("inst1"), curIds.get(0), "Lab 3 released",
                "Lab 3 on recursion is on the course page. Submit before Friday 23:59.");
        AnnouncementDAO.insert(c, users.get("inst4"), curIds.get(7), "Project groups",
                "Form groups of three for the ML project and register them by next Tuesday.");
        Sql.update(c, "UPDATE announcements SET created_at = NOW() - INTERVAL announcement_id DAY");
        AuditDAO.log(c, admin, "admin1", "SETUP", "Demo campus loaded");
    }

    private int section(Connection c, Sec s, String semester, int year) throws SQLException {
        int id = SectionDAO.insert(c, courseIds.get(s.code()), users.get(s.instructor()), s.slot(), s.room(),
                s.capacity(), semester, year);
        GradeDAO.addDefaultComponents(c, id);
        return id;
    }

    /** Picks up to n sections with distinct times. */
    private List<Integer> pick(List<Integer> ids, List<String> slots, int n) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) order.add(i);
        java.util.Collections.shuffle(order, rnd);
        List<Integer> out = new ArrayList<>();
        List<String> used = new ArrayList<>();
        for (int i : order) {
            if (out.size() == n) break;
            boolean clash = used.stream().anyMatch(u -> TimeSlot.clash(u, slots.get(i)));
            if (!clash) {
                out.add(ids.get(i));
                used.add(slots.get(i));
            }
        }
        return out;
    }

    /** Writes marks for an enrollment and returns the weighted percentage. */
    private double markAll(Connection c, int eid, int sec, double ability, boolean includeFinal) throws SQLException {
        double total = 0;
        double weights = 0;
        for (GradeComponent comp : GradeDAO.components(c, sec)) {
            if (!includeFinal && comp.name().equals("Final")) continue;
            if (!includeFinal && rnd.nextDouble() < 0.06) continue; // a few not yet marked
            double pct = clamp(ability + rnd.nextGaussian() * 9, 15, 100);
            double score = Math.round(pct / 100 * comp.maxScore() * 2) / 2.0;
            GradeDAO.upsert(c, eid, comp.name(), score, comp.maxScore(), comp.weight());
            total += score / comp.maxScore() * comp.weight();
            weights += comp.weight();
        }
        return weights == 0 ? 0 : total / weights * 100;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
