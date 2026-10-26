package edu.univ.erp.ui.page.instructor;

import edu.univ.erp.domain.Announcement;
import edu.univ.erp.domain.Section;
import edu.univ.erp.service.InstructorService;
import edu.univ.erp.service.SettingsService;
import edu.univ.erp.ui.chart.BarChart;
import edu.univ.erp.ui.chart.DonutChart;
import edu.univ.erp.ui.kit.Btn;
import edu.univ.erp.ui.kit.EmptyState;
import edu.univ.erp.ui.kit.StatCard;
import edu.univ.erp.ui.page.Page;
import edu.univ.erp.ui.page.Parts;
import edu.univ.erp.ui.page.Shell;
import edu.univ.erp.ui.page.common.NoticesPage;
import edu.univ.erp.ui.theme.Theme;
import edu.univ.erp.ui.three.Bar3DChart;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class InstructorDashboard extends Page {

    private record Data(List<Section> current, List<InstructorService.SectionStats> stats, List<Announcement> notices,
                        String term) {
    }

    public InstructorDashboard(Shell shell) {
        super(shell);
    }

    @Override
    public String title() {
        return "Dashboard";
    }

    @Override
    public String subtitle() {
        return "Your classes at a glance";
    }

    @Override
    public void reload() {
        load(() -> {
            String term = SettingsService.get().refresh().term();
            List<Section> current = InstructorService.sections().stream().filter(s -> s.term().equals(term)).toList();
            List<InstructorService.SectionStats> st = new ArrayList<>();
            for (Section s : current) st.add(InstructorService.stats(s.id()));
            return new Data(current, st, InstructorService.announcements(5), term);
        }, this::render);
    }

    private void render(Data d) {
        JPanel col = column();
        int students = d.stats().stream().mapToInt(InstructorService.SectionStats::students).sum();
        int finalized = d.stats().stream().mapToInt(InstructorService.SectionStats::finalized).sum();
        double avg = d.stats().stream().filter(s -> s.averagePercent() > 0)
                .mapToDouble(InstructorService.SectionStats::averagePercent).average().orElse(0);
        Btn gb = Btn.secondary("Open gradebook", "clipboard");
        gb.addActionListener(e -> shell.go("gradebook"));
        col.add(Parts.hero(Parts.greeting() + ", " + Parts.firstName(shell.user().displayName()),
                "You're teaching <b>" + d.current().size() + " sections</b> with <b>" + students + " students</b> in "
                        + d.term() + ".", gb));

        JPanel stats = new JPanel(new MigLayout("insets 0, fillx, gap 18", "[grow, fill, sg s]".repeat(4), ""));
        stats.setOpaque(false);
        stats.add(new StatCard("layers", Theme.series(0), "Sections this term", d.current().size(),
                v -> String.valueOf(Math.round(v)), d.term()));
        stats.add(new StatCard("users", Theme.series(1), "Students", students, v -> String.valueOf(Math.round(v)),
                "registered across sections"));
        stats.add(new StatCard("trend", Theme.series(2), "Class average", avg, v -> String.format("%.1f%%", v),
                "on marks recorded so far"));
        stats.add(new StatCard("award", Theme.series(3), "Final grades published", finalized,
                v -> String.valueOf(Math.round(v)), "of " + students + " students"));
        col.add(stats);

        if (d.current().isEmpty()) {
            col.add(Parts.section("This term", null, new EmptyState("layers", "No sections this term",
                    "Ask an administrator to assign you to a section.")));
        } else {
            JPanel row = new JPanel(new MigLayout("insets 0, fillx, gap 18", "[grow 58, fill][grow 42, fill]", "[top]"));
            row.setOpaque(false);
            List<String> labels = d.current().stream().map(s -> s.courseCode() + " #" + s.id()).toList();
            BarChart avgChart = new BarChart().max(100).format(v -> String.format("%.0f%%", v)).colorByCategory();
            avgChart.data(labels, "Average", d.stats().stream().mapToDouble(InstructorService.SectionStats::averagePercent)
                    .toArray(), Theme.series(0));
            row.add(Parts.chart("Average score by section", "Weighted, on assessments marked so far", avgChart, 250));
            DonutChart donut = new DonutChart().caption("Students");
            donut.data(labels, d.stats().stream().mapToDouble(InstructorService.SectionStats::students).toArray());
            row.add(Parts.chart("Students by section", null, donut, 250));
            col.add(row);

            LinkedHashSet<String> comps = new LinkedHashSet<>();
            for (InstructorService.SectionStats s : d.stats()) for (var c : s.components()) comps.add(c.name());
            List<String> compList = new ArrayList<>(comps);
            double[][] vals = new double[compList.size()][labels.size()];
            for (int c = 0; c < d.stats().size(); c++) {
                for (var cs : d.stats().get(c).components()) vals[compList.indexOf(cs.name())][c] = cs.avgPercent();
            }
            JPanel row2 = new JPanel(new MigLayout("insets 0, fillx, gap 18", "[grow 62, fill][grow 38, fill]", "[top]"));
            row2.setOpaque(false);
            Bar3DChart c3 = new Bar3DChart().max(100).format(v -> String.format("%.0f%%", v));
            c3.data(labels, compList, vals);
            row2.add(Parts.chart("Assessment averages in 3D", "Each bar is a section's average on one assessment", c3, 330));
            Btn all = Btn.ghost("All notices", "arrow-right").compact();
            all.addActionListener(e -> shell.go("notices"));
            row2.add(Parts.section("Notices", null, NoticesPage.compactList(d.notices(), 4), all));
            col.add(row2);
        }
        setBody(col, true);
    }
}
