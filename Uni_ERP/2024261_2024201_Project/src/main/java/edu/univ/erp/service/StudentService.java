package edu.univ.erp.service;

import edu.univ.erp.data.AnnouncementDAO;
import edu.univ.erp.data.EnrollmentDAO;
import edu.univ.erp.data.GradeDAO;
import edu.univ.erp.data.ProfileDAO;
import edu.univ.erp.data.SectionDAO;
import edu.univ.erp.db.Database;
import edu.univ.erp.domain.Announcement;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.GradeComponent;
import edu.univ.erp.domain.GradeEntry;
import edu.univ.erp.domain.Role;
import edu.univ.erp.domain.Section;
import edu.univ.erp.domain.StudentProfile;
import edu.univ.erp.util.GradeScale;
import edu.univ.erp.util.TimeSlot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Everything a signed-in student can see and do. */
public final class StudentService {

    /** One course's assessment breakdown for the grades page. */
    public record CourseResult(Enrollment enrollment, List<GradeComponent> components, Map<String, GradeEntry> scores) {

        /** Weighted percentage over the components graded so far, or null if nothing is graded. */
        public Double runningPercent() {
            double earned = 0;
            double weight = 0;
            for (GradeComponent comp : components) {
                GradeEntry g = scores.get(comp.name());
                if (g != null && comp.maxScore() > 0) {
                    earned += g.score() / comp.maxScore() * comp.weight();
                    weight += comp.weight();
                }
            }
            return weight == 0 ? null : earned / weight * 100.0;
        }
    }

    /** Grade point averages: cumulative and per term in chronological order. */
    public record GpaSummary(double cgpa, int creditsEarned, int creditsGraded, Map<String, Double> termGpa) {
    }

    private StudentService() {
    }

    private static int me() {
        Session.require(Role.STUDENT);
        return Session.user().id();
    }

    public static StudentProfile profile() {
        int id = me();
        return Database.read(c -> ProfileDAO.student(c, id))
                .orElseThrow(() -> new ServiceException("Your student profile is missing. Contact the administrator."));
    }

    /** Sections offered in the current term. */
    public static List<Section> catalog() {
        me();
        SettingsService s = SettingsService.get().refresh();
        return Database.read(c -> SectionDAO.listByTerm(c, s.semester(), s.year()));
    }

    public static List<Enrollment> enrollments() {
        int id = me();
        return Database.read(c -> EnrollmentDAO.forStudent(c, id));
    }

    /** Active registrations in the current term. */
    public static List<Enrollment> currentRegistrations() {
        SettingsService s = SettingsService.get();
        return enrollments().stream()
                .filter(e -> e.isActive() && e.section().semester().equals(s.semester())
                        && e.section().year() == s.year())
                .toList();
    }

    public static int currentCredits() {
        return currentRegistrations().stream().mapToInt(e -> e.section().credits()).sum();
    }

    /**
     * Registers for a section. The section row is locked for the duration so two students can't
     * take the last seat at the same time; the seat count is recomputed rather than incremented.
     */
    public static Enrollment register(int sectionId) {
        int id = me();
        Session.requireWritable();
        SettingsService settings = SettingsService.get().refresh();
        LocalDate addBy = settings.addDeadline();
        if (addBy != null && LocalDate.now().isAfter(addBy)) {
            throw new ServiceException("Registration closed on " + addBy + ".");
        }
        return Database.tx(c -> {
            SectionDAO.lock(c, sectionId);
            Section section = SectionDAO.find(c, sectionId)
                    .orElseThrow(() -> new ServiceException("That section no longer exists."));
            if (!section.semester().equals(settings.semester()) || section.year() != settings.year()) {
                throw new ServiceException("You can only register for sections in " + settings.term() + ".");
            }
            Optional<Enrollment> existing = EnrollmentDAO.find(c, id, sectionId);
            if (existing.isPresent() && existing.get().isActive()) {
                throw new ServiceException("You're already registered in this section.");
            }
            if (existing.isPresent() && "COMPLETED".equals(existing.get().status())) {
                throw new ServiceException("You've already completed this section.");
            }

            List<Enrollment> mine = EnrollmentDAO.forStudent(c, id).stream()
                    .filter(e -> e.isActive() && e.section().semester().equals(section.semester())
                            && e.section().year() == section.year())
                    .toList();
            for (Enrollment e : mine) {
                if (e.section().courseId() == section.courseId()) {
                    throw new ServiceException("You're already in another section of " + section.courseCode() + ".");
                }
                if (TimeSlot.clash(e.section().dayTime(), section.dayTime())) {
                    throw new ServiceException("Timetable clash with " + e.section().courseCode() + " ("
                            + e.section().dayTime() + ").");
                }
            }
            int credits = mine.stream().mapToInt(e -> e.section().credits()).sum() + section.credits();
            if (credits > settings.maxCredits()) {
                throw new ServiceException("This would take you to " + credits + " credits; the limit is "
                        + settings.maxCredits() + ".");
            }
            int taken = SectionDAO.recount(c, sectionId);
            if (taken >= section.capacity()) {
                throw new ServiceException("Sorry, " + section.courseCode() + " is full.");
            }

            int enrollmentId;
            if (existing.isPresent()) {
                // v1 bug: a dropped row blocked re-registration via the unique key; reuse it instead
                enrollmentId = existing.get().id();
                EnrollmentDAO.reactivate(c, enrollmentId);
            } else {
                enrollmentId = EnrollmentDAO.insert(c, id, sectionId);
            }
            SectionDAO.recount(c, sectionId);
            Audit.log(c, "REGISTER", section.courseCode() + " section " + sectionId);
            return EnrollmentDAO.find(c, enrollmentId).orElseThrow();
        });
    }

    public static void drop(int enrollmentId) {
        int id = me();
        Session.requireWritable();
        LocalDate dropBy = SettingsService.get().refresh().dropDeadline();
        if (dropBy != null && LocalDate.now().isAfter(dropBy)) {
            throw new ServiceException("The drop deadline (" + dropBy + ") has passed.");
        }
        Database.txRun(c -> {
            Enrollment e = EnrollmentDAO.find(c, enrollmentId)
                    .orElseThrow(() -> new ServiceException("That registration no longer exists."));
            if (e.studentId() != id) throw new ServiceException("That registration isn't yours.");
            if (!e.isActive()) throw new ServiceException("This course isn't currently registered.");
            if (e.hasFinal()) throw new ServiceException("Final grades are already out for this course.");
            SectionDAO.lock(c, e.sectionId());
            EnrollmentDAO.setStatus(c, enrollmentId, "DROPPED");
            SectionDAO.recount(c, e.sectionId());
            Audit.log(c, "DROP", e.section().courseCode() + " section " + e.sectionId());
        });
    }

    /** Assessment breakdown for every course the student is or was registered in (not dropped). */
    public static List<CourseResult> results() {
        int id = me();
        return Database.read(c -> {
            List<CourseResult> out = new ArrayList<>();
            for (Enrollment e : EnrollmentDAO.forStudent(c, id)) {
                if ("DROPPED".equals(e.status())) continue;
                Map<String, GradeEntry> scores = new LinkedHashMap<>();
                for (GradeEntry g : GradeDAO.forEnrollment(c, e.id())) scores.put(g.component(), g);
                out.add(new CourseResult(e, GradeDAO.components(c, e.sectionId()), scores));
            }
            return out;
        });
    }

    public static GpaSummary gpa() {
        return gpa(enrollments());
    }

    static GpaSummary gpa(List<Enrollment> enrollments) {
        double points = 0;
        int graded = 0;
        int earned = 0;
        Map<String, double[]> terms = new LinkedHashMap<>();
        List<Enrollment> sorted = new ArrayList<>(enrollments);
        sorted.sort((a, b) -> a.section().year() != b.section().year()
                ? Integer.compare(a.section().year(), b.section().year())
                : termOrder(a.section().semester()) - termOrder(b.section().semester()));
        for (Enrollment e : sorted) {
            if (!e.hasFinal() || "DROPPED".equals(e.status())) continue;
            int cr = e.section().credits();
            double p = GradeScale.points(e.finalGrade());
            points += p * cr;
            graded += cr;
            if (p > 0) earned += cr;
            double[] t = terms.computeIfAbsent(e.section().term(), k -> new double[2]);
            t[0] += p * cr;
            t[1] += cr;
        }
        Map<String, Double> termGpa = new LinkedHashMap<>();
        terms.forEach((k, v) -> termGpa.put(k, v[1] == 0 ? 0 : v[0] / v[1]));
        return new GpaSummary(graded == 0 ? 0 : points / graded, earned, graded, termGpa);
    }

    static int termOrder(String semester) {
        String s = semester == null ? "" : semester.toLowerCase();
        if (s.startsWith("winter") || s.startsWith("spring")) return 0;
        if (s.startsWith("summer")) return 1;
        return 2; // monsoon / fall
    }

    public static List<Announcement> announcements(int limit) {
        int id = me();
        return Database.read(c -> AnnouncementDAO.forStudent(c, id, limit));
    }
}
