package edu.univ.erp.service;

import edu.univ.erp.data.AnnouncementDAO;
import edu.univ.erp.data.EnrollmentDAO;
import edu.univ.erp.data.GradeDAO;
import edu.univ.erp.data.ProfileDAO;
import edu.univ.erp.data.SectionDAO;
import edu.univ.erp.db.Database;
import edu.univ.erp.db.Sql;
import edu.univ.erp.domain.Announcement;
import edu.univ.erp.domain.GradeComponent;
import edu.univ.erp.domain.GradeEntry;
import edu.univ.erp.domain.InstructorProfile;
import edu.univ.erp.domain.Role;
import edu.univ.erp.domain.RosterEntry;
import edu.univ.erp.domain.Section;
import edu.univ.erp.util.Csv;
import edu.univ.erp.util.GradeScale;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Everything a signed-in instructor can see and do. */
public final class InstructorService {

    public record Gradebook(Section section, List<GradeComponent> components, List<RosterEntry> roster,
                            Map<Integer, Map<String, GradeEntry>> scores) {
    }

    public record ComponentStats(String name, double maxScore, double weight, int graded, double avgPercent,
                                 double minPercent, double maxPercent, double stdDevPercent) {
    }

    public record SectionStats(Section section, int students, int finalized, double averagePercent,
                               List<ComponentStats> components, int[] histogram, Map<String, Integer> letters) {
    }

    public record FinalizeResult(int computed, int incomplete) {
    }

    private InstructorService() {
    }

    private static int me() {
        Session.require(Role.INSTRUCTOR);
        return Session.user().id();
    }

    public static InstructorProfile profile() {
        int id = me();
        return Database.read(c -> ProfileDAO.instructor(c, id))
                .orElseThrow(() -> new ServiceException("Your instructor profile is missing. Contact the administrator."));
    }

    public static List<Section> sections() {
        int id = me();
        return Database.read(c -> SectionDAO.listByInstructor(c, id));
    }

    /** Loads a section and checks the signed-in instructor teaches it. */
    private static Section owned(Connection c, int sectionId) throws SQLException {
        int id = me();
        Section s = SectionDAO.find(c, sectionId).orElseThrow(() -> new ServiceException("Section not found."));
        if (s.instructorId() == null || s.instructorId() != id) {
            throw new ServiceException("You don't teach this section.");
        }
        return s;
    }

    public static Gradebook gradebook(int sectionId) {
        return Database.read(c -> {
            Section s = owned(c, sectionId);
            List<GradeComponent> comps = GradeDAO.components(c, sectionId);
            return new Gradebook(s, comps, EnrollmentDAO.roster(c, sectionId), GradeDAO.forSection(c, sectionId));
        });
    }

    /**
     * Saves scores. A null score clears that cell. Scores are validated against each
     * component's maximum, and any student whose final grade was already published has it
     * recalculated so it never goes stale.
     */
    public static int saveScores(int sectionId, Map<Integer, Map<String, Double>> changes) {
        Session.requireWritable();
        return Database.tx(c -> {
            Section s = owned(c, sectionId);
            Map<String, GradeComponent> comps = byName(GradeDAO.components(c, sectionId));
            Set<Integer> roster = new HashSet<>();
            Map<Integer, RosterEntry> rows = new HashMap<>();
            for (RosterEntry r : EnrollmentDAO.roster(c, sectionId)) {
                roster.add(r.enrollmentId());
                rows.put(r.enrollmentId(), r);
            }
            int cells = 0;
            for (var row : changes.entrySet()) {
                int enrollmentId = row.getKey();
                if (!roster.contains(enrollmentId)) throw new ServiceException("A student is no longer in this section.");
                for (var cell : row.getValue().entrySet()) {
                    GradeComponent comp = comps.get(cell.getKey());
                    if (comp == null) throw new ServiceException("Unknown component " + cell.getKey() + ".");
                    Double v = cell.getValue();
                    if (v == null) {
                        GradeDAO.delete(c, enrollmentId, comp.name());
                    } else {
                        if (v < 0 || v > comp.maxScore()) {
                            throw new ServiceException(rows.get(enrollmentId).name() + ": " + comp.name()
                                    + " must be between 0 and " + fmt(comp.maxScore()) + ".");
                        }
                        GradeDAO.upsert(c, enrollmentId, comp.name(), v, comp.maxScore(), comp.weight());
                    }
                    cells++;
                }
            }
            refreshPublishedFinals(c, sectionId, changes.keySet());
            Audit.log(c, "GRADES_SAVED", s.courseCode() + " section " + sectionId + ": " + cells + " cells");
            return cells;
        });
    }

    /** Replaces the section's assessment scheme. Weights must add up to 100. */
    public static void saveComponents(int sectionId, List<GradeComponent> comps) {
        Session.requireWritable();
        if (comps.isEmpty()) throw new ServiceException("Add at least one assessment.");
        double total = 0;
        Set<String> names = new HashSet<>();
        for (GradeComponent g : comps) {
            String n = g.name() == null ? "" : g.name().trim();
            if (n.isEmpty()) throw new ServiceException("Every assessment needs a name.");
            if (n.length() > 50) throw new ServiceException("Assessment names must be 50 characters or fewer.");
            if (!names.add(n.toLowerCase())) throw new ServiceException("Assessment names must be unique (" + n + ").");
            if (g.maxScore() <= 0 || g.maxScore() > 1000) throw new ServiceException(n + ": maximum marks must be 1–1000.");
            if (g.weight() <= 0 || g.weight() > 100) throw new ServiceException(n + ": weight must be between 0 and 100.");
            total += g.weight();
        }
        if (Math.abs(total - 100) > 0.01) {
            throw new ServiceException("Weights must add up to 100% (currently " + fmt(total) + "%).");
        }
        Database.txRun(c -> {
            Section s = owned(c, sectionId);
            List<GradeComponent> clean = comps.stream()
                    .map(g -> new GradeComponent(0, sectionId, g.name().trim(), g.maxScore(), g.weight(), 0)).toList();
            // Scores already above a lowered maximum would become invalid
            for (GradeComponent g : clean) {
                int over = Sql.count(c, "SELECT COUNT(*) FROM grades gr JOIN enrollments e ON e.enrollment_id = "
                        + "gr.enrollment_id WHERE e.section_id = ? AND gr.component = ? AND gr.score > ?",
                        sectionId, g.name(), g.maxScore());
                if (over > 0) {
                    throw new ServiceException(over + " recorded " + g.name() + " score(s) exceed the new maximum of "
                            + fmt(g.maxScore()) + ".");
                }
            }
            GradeDAO.replaceComponents(c, sectionId, clean);
            GradeDAO.deleteOrphans(c, sectionId);
            Sql.update(c, "UPDATE grades g JOIN enrollments e ON e.enrollment_id = g.enrollment_id "
                    + "JOIN section_components sc ON sc.section_id = e.section_id AND sc.name = g.component "
                    + "SET g.max_score = sc.max_score, g.weight = sc.weight WHERE e.section_id = ?", sectionId);
            refreshPublishedFinals(c, sectionId, null);
            Audit.log(c, "SCHEME_CHANGED", s.courseCode() + " section " + sectionId + ": " + clean.size() + " assessments");
        });
    }

    /** Computes and publishes final grades for every student with all assessments marked. */
    public static FinalizeResult computeFinals(int sectionId) {
        Session.requireWritable();
        return Database.tx(c -> {
            Section s = owned(c, sectionId);
            List<GradeComponent> comps = GradeDAO.components(c, sectionId);
            Map<Integer, Map<String, GradeEntry>> scores = GradeDAO.forSection(c, sectionId);
            int done = 0;
            int incomplete = 0;
            for (RosterEntry r : EnrollmentDAO.roster(c, sectionId)) {
                Double pct = finalPercent(comps, scores.get(r.enrollmentId()));
                if (pct == null) {
                    incomplete++;
                    continue;
                }
                EnrollmentDAO.setFinal(c, r.enrollmentId(), GradeScale.letter(pct), round1(pct));
                done++;
            }
            Audit.log(c, "FINALS_PUBLISHED", s.courseCode() + " section " + sectionId + ": " + done + " students");
            return new FinalizeResult(done, incomplete);
        });
    }

    private static void refreshPublishedFinals(Connection c, int sectionId, Set<Integer> only) throws SQLException {
        List<GradeComponent> comps = GradeDAO.components(c, sectionId);
        Map<Integer, Map<String, GradeEntry>> scores = GradeDAO.forSection(c, sectionId);
        for (RosterEntry r : EnrollmentDAO.roster(c, sectionId)) {
            if (r.finalGrade() == null || only != null && !only.contains(r.enrollmentId())) continue;
            Double pct = finalPercent(comps, scores.get(r.enrollmentId()));
            if (pct == null) {
                EnrollmentDAO.setFinal(c, r.enrollmentId(), null, null);
            } else {
                EnrollmentDAO.setFinal(c, r.enrollmentId(), GradeScale.letter(pct), round1(pct));
            }
        }
    }

    /** Weighted percentage, or null when any assessment is still unmarked. */
    public static Double finalPercent(List<GradeComponent> comps, Map<String, GradeEntry> scores) {
        if (comps.isEmpty() || scores == null) return null;
        double total = 0;
        double weights = 0;
        for (GradeComponent comp : comps) {
            GradeEntry g = scores.get(comp.name());
            if (g == null) return null;
            total += g.score() / comp.maxScore() * comp.weight();
            weights += comp.weight();
        }
        return weights == 0 ? null : total / weights * 100.0;
    }

    /** Weighted percentage over the assessments marked so far, or null if none are marked. */
    public static Double runningPercent(List<GradeComponent> comps, Map<String, GradeEntry> scores) {
        if (scores == null) return null;
        double total = 0;
        double weights = 0;
        for (GradeComponent comp : comps) {
            GradeEntry g = scores.get(comp.name());
            if (g == null) continue;
            total += g.score() / comp.maxScore() * comp.weight();
            weights += comp.weight();
        }
        return weights == 0 ? null : total / weights * 100.0;
    }

    public static SectionStats stats(int sectionId) {
        Gradebook gb = gradebook(sectionId);
        List<ComponentStats> comps = new ArrayList<>();
        for (GradeComponent comp : gb.components()) {
            List<Double> pcts = new ArrayList<>();
            for (RosterEntry r : gb.roster()) {
                GradeEntry g = gb.scores().getOrDefault(r.enrollmentId(), Map.of()).get(comp.name());
                if (g != null) pcts.add(g.score() / comp.maxScore() * 100.0);
            }
            double avg = pcts.stream().mapToDouble(d -> d).average().orElse(0);
            double min = pcts.stream().mapToDouble(d -> d).min().orElse(0);
            double max = pcts.stream().mapToDouble(d -> d).max().orElse(0);
            double var = pcts.stream().mapToDouble(d -> (d - avg) * (d - avg)).average().orElse(0);
            comps.add(new ComponentStats(comp.name(), comp.maxScore(), comp.weight(), pcts.size(), avg, min, max,
                    Math.sqrt(var)));
        }
        int[] hist = new int[10];
        Map<String, Integer> letters = new LinkedHashMap<>();
        for (GradeScale.Band b : GradeScale.BANDS) letters.put(b.letter(), 0);
        double sum = 0;
        int n = 0;
        int finalized = 0;
        for (RosterEntry r : gb.roster()) {
            Double pct = r.finalPercentage();
            if (pct == null) pct = runningPercent(gb.components(), gb.scores().get(r.enrollmentId()));
            if (pct != null) {
                hist[Math.min(9, (int) (pct / 10))]++;
                sum += pct;
                n++;
            }
            if (r.finalGrade() != null) {
                finalized++;
                letters.merge(r.finalGrade(), 1, Integer::sum);
            }
        }
        return new SectionStats(gb.section(), gb.roster().size(), finalized, n == 0 ? 0 : sum / n, comps, hist, letters);
    }

    public static void exportCsv(int sectionId, Path file) throws IOException {
        Gradebook gb = gradebook(sectionId);
        List<String> header = new ArrayList<>(List.of("Roll No", "Name", "Program"));
        for (GradeComponent comp : gb.components()) header.add(comp.name() + " (/" + fmt(comp.maxScore()) + ")");
        header.add("Final %");
        header.add("Grade");
        List<List<Object>> rows = new ArrayList<>();
        for (RosterEntry r : gb.roster()) {
            List<Object> row = new ArrayList<>(List.of(r.rollNo(), r.name(), r.program() == null ? "" : r.program()));
            Map<String, GradeEntry> sc = gb.scores().getOrDefault(r.enrollmentId(), Map.of());
            for (GradeComponent comp : gb.components()) {
                GradeEntry g = sc.get(comp.name());
                row.add(g == null ? "" : fmt(g.score()));
            }
            row.add(r.finalPercentage() == null ? "" : String.format("%.1f", r.finalPercentage()));
            row.add(r.finalGrade() == null ? "" : r.finalGrade());
            rows.add(row);
        }
        Csv.write(file, header, rows);
    }

    public static List<Announcement> announcements(int limit) {
        int id = me();
        return Database.read(c -> AnnouncementDAO.forInstructor(c, id, limit));
    }

    public static void postAnnouncement(int sectionId, String title, String body) {
        Session.requireWritable();
        String t = Validate.required(title, "Title");
        String b = Validate.required(body, "Message");
        if (t.length() > 150) throw new ServiceException("Title must be 150 characters or fewer.");
        Database.txRun(c -> {
            Section s = owned(c, sectionId);
            AnnouncementDAO.insert(c, Session.user().id(), sectionId, t, b);
            Audit.log(c, "ANNOUNCEMENT", s.courseCode() + ": " + t);
        });
    }

    public static void deleteAnnouncement(int id) {
        int me = me();
        Database.txRun(c -> {
            if (AnnouncementDAO.authorOf(c, id) != me) throw new ServiceException("You can only delete your own notices.");
            AnnouncementDAO.delete(c, id);
            Audit.log(c, "ANNOUNCEMENT_DELETED", "#" + id);
        });
    }

    private static Map<String, GradeComponent> byName(List<GradeComponent> comps) {
        Map<String, GradeComponent> m = new LinkedHashMap<>();
        for (GradeComponent g : comps) m.put(g.name(), g);
        return m;
    }

    static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format("%.2f", v);
    }
}
