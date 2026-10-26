package edu.univ.erp.ui.page.admin;

import edu.univ.erp.domain.AuditEntry;
import edu.univ.erp.domain.LabelValue;
import edu.univ.erp.service.AdminService;
import edu.univ.erp.service.SettingsService;
import edu.univ.erp.ui.chart.BarChart;
import edu.univ.erp.ui.chart.DonutChart;
import edu.univ.erp.ui.chart.Gauge;
import edu.univ.erp.ui.chart.HBarChart;
import edu.univ.erp.ui.chart.LineChart;
import edu.univ.erp.ui.kit.Btn;
import edu.univ.erp.ui.kit.StatCard;
import edu.univ.erp.ui.kit.UI;
import edu.univ.erp.ui.page.Page;
import edu.univ.erp.ui.page.Parts;
import edu.univ.erp.ui.page.Shell;
import edu.univ.erp.ui.theme.Theme;
import edu.univ.erp.ui.three.Bar3DChart;
import edu.univ.erp.util.GradeScale;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class AdminDashboard extends Page {

    public AdminDashboard(Shell shell) {
        super(shell);
    }

    @Override
    public String title() {
        return "Dashboard";
    }

    @Override
    public String subtitle() {
        return SettingsService.get().institution() + " · " + SettingsService.get().term();
    }

    @Override
    public void reload() {
        load(AdminService::dashboard, this::render);
    }

    private void render(AdminService.Dashboard d) {
        JPanel col = column();
        Map<String, Integer> roles = d.usersByRole();
        Btn users = Btn.secondary("Manage users", "users");
        users.addActionListener(e -> shell.go("users"));
        col.add(Parts.hero(Parts.greeting() + ", " + Parts.firstName(shell.user().displayName()),
                SettingsService.get().term() + ": <b>" + d.sectionsThisTerm() + " sections</b>, <b>" + d.activeEnrollments()
                        + " active registrations</b> and " + Math.round(d.capacityUsed() * 100) + "% of seats filled.", users));

        JPanel stats = new JPanel(new MigLayout("insets 0, fillx, gap 18", "[grow, fill, sg s]".repeat(4), ""));
        stats.setOpaque(false);
        stats.add(new StatCard("cap", Theme.series(0), "Students", roles.getOrDefault("STUDENT", 0),
                v -> String.valueOf(Math.round(v)), "enrolled at " + SettingsService.get().institution()));
        stats.add(new StatCard("book", Theme.series(1), "Instructors", roles.getOrDefault("INSTRUCTOR", 0),
                v -> String.valueOf(Math.round(v)), roles.getOrDefault("ADMIN", 0) + " administrators"));
        stats.add(new StatCard("layers", Theme.series(2), "Courses", d.courses(), v -> String.valueOf(Math.round(v)),
                d.sectionsThisTerm() + " sections this term"));
        stats.add(new StatCard("check", Theme.series(3), "Active registrations", d.activeEnrollments(),
                v -> String.valueOf(Math.round(v)), "across all sections"));
        col.add(stats);

        JPanel a = new JPanel(new MigLayout("insets 0, fillx, gap 18", "[grow 52, fill][grow 18, fill][grow 30, fill]", "[top]"));
        a.setOpaque(false);
        LineChart trend = new LineChart().labelEvery(5);
        trend.data(d.registrationTrend().stream().map(LabelValue::label).toList(), "Registrations",
                values(d.registrationTrend()), Theme.series(0));
        a.add(Parts.chart("Registrations", "Last 30 days", trend, 250));
        Gauge cap = new Gauge().set(d.capacityUsed(), 1, Math.round(d.capacityUsed() * 100) + "%", "seats filled")
                .colors(Theme.series(2), Theme.series(1));
        a.add(Parts.chart("Capacity", "This term", cap, 250));
        HBarChart prog = new HBarChart();
        prog.data(d.studentsByProgram().stream().map(LabelValue::label).toList(), values(d.studentsByProgram()));
        a.add(Parts.chart("Students by program", null, prog, 250));
        col.add(a);

        JPanel b = new JPanel(new MigLayout("insets 0, fillx, gap 18", "[grow 56, fill][grow 44, fill]", "[top]"));
        b.setOpaque(false);
        BarChart courses = new BarChart().colorByCategory();
        courses.data(d.enrollmentsByCourse().stream().map(LabelValue::label).toList(), "Students",
                values(d.enrollmentsByCourse()), Theme.series(0));
        b.add(Parts.chart("Most popular courses", "Registered students this term", courses, 260));
        List<String> letters = new ArrayList<>();
        List<Double> counts = new ArrayList<>();
        List<Color> colors = new ArrayList<>();
        for (GradeScale.Band band : GradeScale.BANDS) {
            for (LabelValue lv : d.gradeDistribution()) {
                if (lv.label().equals(band.letter())) {
                    letters.add(lv.label());
                    counts.add(lv.value());
                    colors.add(Parts.gradeColor(lv.label()));
                }
            }
        }
        DonutChart grades = new DonutChart().caption("Grades").colors(colors.toArray(new Color[0]));
        grades.data(letters, counts.stream().mapToDouble(x -> x).toArray());
        b.add(Parts.chart("Grade distribution", "All published final grades", grades, 260));
        col.add(b);

        JPanel c = new JPanel(new MigLayout("insets 0, fillx, gap 18", "[grow 62, fill][grow 38, fill]", "[top]"));
        c.setOpaque(false);
        TreeSet<String> depts = new TreeSet<>();
        TreeSet<String> progs = new TreeSet<>();
        for (Object[] r : d.deptByProgram()) {
            depts.add((String) r[0]);
            progs.add((String) r[1]);
        }
        List<String> dl = new ArrayList<>(depts);
        List<String> pl = new ArrayList<>(progs);
        double[][] grid = new double[pl.size()][dl.size()];
        for (Object[] r : d.deptByProgram()) grid[pl.indexOf((String) r[1])][dl.indexOf((String) r[0])] = (Double) r[2];
        Bar3DChart c3 = new Bar3DChart();
        c3.data(dl.stream().map(AdminDashboard::shortDept).toList(), pl, grid);
        c.add(Parts.chart("Where students study", "Registrations by course department and student program", c3, 340));
        Btn all = Btn.ghost("Audit log", "arrow-right").compact();
        all.addActionListener(e -> shell.go("audit"));
        c.add(Parts.section("Recent activity", null, activity(d.recentActivity()), all));
        col.add(c);
        setBody(col, true);
    }

    static String shortDept(String d) {
        return switch (d) {
            case "Computer Science" -> "CS";
            case "Mathematics" -> "Math";
            case "Electronics" -> "ECE";
            case "Social Sciences" -> "Soc Sci";
            case "Student Affairs" -> "Affairs";
            default -> d.length() > 10 ? d.substring(0, 9) + "…" : d;
        };
    }

    private static double[] values(List<LabelValue> l) {
        return l.stream().mapToDouble(LabelValue::value).toArray();
    }

    private static JComponent activity(List<AuditEntry> entries) {
        JPanel p = new JPanel(new MigLayout("insets 0, fillx, wrap 1, gapy 12", "[grow, fill]", ""));
        p.setOpaque(false);
        for (AuditEntry e : entries) {
            JPanel r = new JPanel(new MigLayout("insets 0, fillx", "[]12[grow]", "[]0[]"));
            r.setOpaque(false);
            r.add(Parts.bubble(AuditPage.icon(e.action()), AuditPage.color(e.action()), 32), "spany 2, aligny top");
            r.add(UI.h3(AuditPage.label(e.action()) + (e.details() == null ? "" : " · " + e.details())), "wmin 0, wrap");
            r.add(UI.muted("@" + e.username() + " · " + Shell.ago(e.createdAt())), "cell 1 1");
            p.add(r);
        }
        if (entries.isEmpty()) p.add(UI.muted("Nothing yet."));
        return p;
    }
}
