package edu.univ.erp.ui.page.student;

import edu.univ.erp.domain.Announcement;
import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.GradeComponent;
import edu.univ.erp.domain.GradeEntry;
import edu.univ.erp.service.SettingsService;
import edu.univ.erp.service.StudentService;
import edu.univ.erp.ui.chart.BarChart;
import edu.univ.erp.ui.chart.LineChart;
import edu.univ.erp.ui.kit.Btn;
import edu.univ.erp.ui.kit.EmptyState;
import edu.univ.erp.ui.kit.StatCard;
import edu.univ.erp.ui.kit.UI;
import edu.univ.erp.ui.page.Page;
import edu.univ.erp.ui.page.Parts;
import edu.univ.erp.ui.page.Shell;
import edu.univ.erp.ui.page.common.NoticesPage;
import edu.univ.erp.ui.theme.Theme;
import edu.univ.erp.ui.three.Bar3DChart;
import edu.univ.erp.util.TimeSlot;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class StudentDashboard extends Page {

    private record Data(List<Enrollment> current, List<StudentService.CourseResult> results,
                        StudentService.GpaSummary gpa, List<Announcement> notices, int maxCredits, String term) {
    }

    public StudentDashboard(Shell shell) {
        super(shell);
    }

    @Override
    public String title() {
        return "Dashboard";
    }

    @Override
    public String subtitle() {
        return "Your term at a glance";
    }

    @Override
    public void reload() {
        load(() -> {
            SettingsService s = SettingsService.get().refresh();
            return new Data(StudentService.currentRegistrations(), StudentService.results(), StudentService.gpa(),
                    StudentService.announcements(5), s.maxCredits(), s.term());
        }, this::render);
    }

    private void render(Data d) {
        JPanel col = column();
        int credits = d.current().stream().mapToInt(e -> e.section().credits()).sum();
        String next = nextClass(d.current());
        Btn browse = Btn.secondary("Browse catalog", "book");
        browse.addActionListener(e -> shell.go("catalog"));
        col.add(Parts.hero(Parts.greeting() + ", " + Parts.firstName(shell.user().displayName()),
                "You're registered for <b>" + d.current().size() + " courses</b> (" + credits + " credits) in "
                        + d.term() + ". " + next, browse));

        JPanel stats = new JPanel(new MigLayout("insets 0, fillx, gap 18", "[grow, fill, sg s]".repeat(4), ""));
        stats.setOpaque(false);
        stats.add(new StatCard("award", Theme.series(0), "Cumulative GPA", d.gpa().cgpa(), v -> String.format("%.2f", v),
                "on a 10-point scale"));
        stats.add(new StatCard("layers", Theme.series(1), "Credits this term", credits, v -> String.valueOf(Math.round(v)),
                "limit " + d.maxCredits()));
        stats.add(new StatCard("book", Theme.series(2), "Courses registered", d.current().size(),
                v -> String.valueOf(Math.round(v)), d.term()));
        stats.add(new StatCard("cap", Theme.series(3), "Credits earned", d.gpa().creditsEarned(),
                v -> String.valueOf(Math.round(v)), "across all terms"));
        col.add(stats);

        // Current performance and GPA trend
        List<StudentService.CourseResult> current = d.results().stream()
                .filter(r -> d.current().stream().anyMatch(e -> e.id() == r.enrollment().id())).toList();
        JPanel row = new JPanel(new MigLayout("insets 0, fillx, gap 18", "[grow 60, fill][grow 40, fill]", "[top]"));
        row.setOpaque(false);
        if (!current.isEmpty()) {
            List<String> labels = new ArrayList<>();
            double[] vals = new double[current.size()];
            for (int i = 0; i < current.size(); i++) {
                labels.add(current.get(i).enrollment().section().courseCode());
                Double p = current.get(i).runningPercent();
                vals[i] = p == null ? 0 : p;
            }
            BarChart bc = new BarChart().max(100).format(v -> String.format("%.0f%%", v)).colorByCategory();
            bc.data(labels, "Score", vals, Theme.series(0));
            row.add(Parts.chart("Performance this term", "Weighted score on assessments marked so far", bc, 250));
        } else {
            row.add(Parts.section("Performance this term", null, new EmptyState("book", "No courses yet",
                    "Register for courses in the catalog to see your progress here.")));
        }
        Map<String, Double> tg = d.gpa().termGpa();
        if (!tg.isEmpty()) {
            LineChart lc = new LineChart().range(0, 10).format(v -> String.format("%.1f", v));
            lc.data(new ArrayList<>(tg.keySet()), "Term GPA", tg.values().stream().mapToDouble(x -> x).toArray(),
                    Theme.series(5));
            row.add(Parts.chart("GPA by term", "Grade points per credit", lc, 250));
        } else {
            row.add(Parts.section("GPA by term", null, new EmptyState("trend", "No grades yet",
                    "Your term GPA appears once final grades are published.")));
        }
        col.add(row);

        JPanel row2 = new JPanel(new MigLayout("insets 0, fillx, gap 18", "[grow 62, fill][grow 38, fill]", "[top]"));
        row2.setOpaque(false);
        if (!current.isEmpty()) {
            LinkedHashSet<String> comps = new LinkedHashSet<>();
            for (StudentService.CourseResult r : current) for (GradeComponent c : r.components()) comps.add(c.name());
            List<String> compList = new ArrayList<>(comps);
            List<String> courses = current.stream().map(r -> r.enrollment().section().courseCode()).toList();
            double[][] vals = new double[compList.size()][courses.size()];
            for (int c = 0; c < courses.size(); c++) {
                StudentService.CourseResult r = current.get(c);
                for (int k = 0; k < compList.size(); k++) {
                    GradeEntry g = r.scores().get(compList.get(k));
                    vals[k][c] = g == null ? 0 : g.percent();
                }
            }
            Bar3DChart chart3d = new Bar3DChart().max(100).format(v -> String.format("%.0f%%", v));
            chart3d.data(courses, compList, vals);
            row2.add(Parts.chart("Assessment landscape", "Every score this term, in 3D", chart3d, 330));
        }
        JPanel side = new JPanel(new MigLayout("insets 0, fillx, wrap 1, gap 18", "[grow, fill]", ""));
        side.setOpaque(false);
        side.add(Parts.section("Today", LocalDate.now().getDayOfWeek().toString().charAt(0)
                + LocalDate.now().getDayOfWeek().toString().substring(1).toLowerCase(), today(d.current())));
        Btn all = Btn.ghost("All notices", "arrow-right").compact();
        all.addActionListener(e -> shell.go("notices"));
        side.add(Parts.section("Notices", null, NoticesPage.compactList(d.notices(), 3), all));
        row2.add(side, current.isEmpty() ? "span 2" : "");
        col.add(row2);
        setBody(col, true);
    }

    private JComponent today(List<Enrollment> regs) {
        DayOfWeek day = LocalDate.now().getDayOfWeek();
        List<Enrollment> todays = new ArrayList<>();
        for (Enrollment e : regs) {
            TimeSlot t = TimeSlot.parse(e.section().dayTime());
            if (t != null && t.days().contains(day)) todays.add(e);
        }
        todays.sort(Comparator.comparingInt(e -> TimeSlot.parse(e.section().dayTime()).startMinute()));
        JPanel p = new JPanel(new MigLayout("insets 0, fillx, wrap 1, gapy 10", "[grow, fill]", ""));
        p.setOpaque(false);
        if (todays.isEmpty()) {
            p.add(UI.muted("No classes today. Enjoy the break!"));
            return p;
        }
        int now = LocalTime.now().getHour() * 60 + LocalTime.now().getMinute();
        for (Enrollment e : todays) {
            TimeSlot t = TimeSlot.parse(e.section().dayTime());
            boolean live = now >= t.startMinute() && now < t.endMinute();
            boolean done = now >= t.endMinute();
            JPanel r = new JPanel(new MigLayout("insets 0, fillx", "[]12[grow]push[]", "[]0[]"));
            r.setOpaque(false);
            r.add(Parts.bubble("clock", live ? Theme.p().success() : done ? Theme.p().subtle() : Theme.series(0), 36),
                    "spany 2");
            r.add(UI.h3(e.section().courseCode() + " · " + e.section().courseTitle()), "wmin 0");
            r.add(new edu.univ.erp.ui.kit.Badge(live ? "Now" : done ? "Done" : "Upcoming",
                    live ? Theme.p().success() : done ? Theme.p().subtle() : Theme.series(0)), "spany 2, wrap");
            r.add(UI.muted(TimeSlot.format(t.startMinute()) + "–" + TimeSlot.format(t.endMinute()) + "  ·  "
                    + e.section().room()), "cell 1 1");
            p.add(r);
        }
        return p;
    }

    private static String nextClass(List<Enrollment> regs) {
        LocalDate today = LocalDate.now();
        int now = LocalTime.now().getHour() * 60 + LocalTime.now().getMinute();
        for (int d = 0; d < 7; d++) {
            DayOfWeek day = today.plusDays(d).getDayOfWeek();
            Enrollment best = null;
            int bestStart = Integer.MAX_VALUE;
            for (Enrollment e : regs) {
                TimeSlot t = TimeSlot.parse(e.section().dayTime());
                if (t == null || !t.days().contains(day)) continue;
                if (d == 0 && t.startMinute() <= now) continue;
                if (t.startMinute() < bestStart) {
                    bestStart = t.startMinute();
                    best = e;
                }
            }
            if (best != null) {
                String when = d == 0 ? "today" : d == 1 ? "tomorrow" : day.toString().charAt(0) + day.toString().substring(1, 3).toLowerCase();
                return "Next up: <b>" + best.section().courseCode() + "</b> " + when + " at " + TimeSlot.format(bestStart)
                        + " in " + best.section().room() + ".";
            }
        }
        return regs.isEmpty() ? "Head to the catalog to pick your courses." : "";
    }
}
