package edu.univ.erp.ui.page.instructor;

import edu.univ.erp.domain.Section;
import edu.univ.erp.service.InstructorService;
import edu.univ.erp.ui.chart.BarChart;
import edu.univ.erp.ui.chart.DonutChart;
import edu.univ.erp.ui.chart.RadarChart;
import edu.univ.erp.ui.kit.DataTable;
import edu.univ.erp.ui.kit.EmptyState;
import edu.univ.erp.ui.kit.Select;
import edu.univ.erp.ui.kit.StatCard;
import edu.univ.erp.ui.page.Page;
import edu.univ.erp.ui.page.Parts;
import edu.univ.erp.ui.page.Shell;
import edu.univ.erp.ui.theme.Theme;
import edu.univ.erp.ui.three.Bar3DChart;
import edu.univ.erp.util.GradeScale;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;

/** Distribution, per-assessment statistics and a 3D view for one section. */
public class AnalyticsPage extends Page {

    private List<Section> sections = List.of();
    private InstructorService.SectionStats stats;

    public AnalyticsPage(Shell shell) {
        super(shell);
    }

    @Override
    public String title() {
        return "Class analytics";
    }

    @Override
    public String subtitle() {
        return stats == null ? "How each class is doing" : stats.section().courseCode() + " · " + stats.section().term();
    }

    @Override
    public void reload() {
        load(() -> {
            List<Section> s = InstructorService.sections();
            Integer id = Selection.sectionId;
            List<Section> ordered = Selection.order(s);
            if (id == null && !ordered.isEmpty()) id = ordered.get(0).id();
            return new Object[]{s, id == null ? null : InstructorService.stats(id)};
        }, r -> {
            @SuppressWarnings("unchecked")
            List<Section> s = (List<Section>) r[0];
            sections = s;
            stats = (InstructorService.SectionStats) r[1];
            shell.refreshTitle(this);
            render();
        });
    }

    private void render() {
        JPanel col = column();
        if (stats == null) {
            col.add(new EmptyState("bars", "No sections yet", "Analytics appear once you have a section."));
            setBody(col, true);
            return;
        }
        Select<Section> picker = Selection.picker(sections);
        picker.onChange(s -> reload());
        col.add(picker, "w 420!, h 40!");

        double best = stats.components().stream().mapToDouble(InstructorService.ComponentStats::avgPercent).max().orElse(0);
        String bestName = stats.components().stream().filter(c -> c.avgPercent() == best).map(InstructorService.ComponentStats::name)
                .findFirst().orElse("—");
        double sd = stats.components().stream().mapToDouble(InstructorService.ComponentStats::stdDevPercent).average().orElse(0);
        JPanel cards = new JPanel(new MigLayout("insets 0, fillx, gap 18", "[grow, fill, sg s]".repeat(4), ""));
        cards.setOpaque(false);
        cards.add(new StatCard("users", Theme.series(0), "Students", stats.students(), v -> String.valueOf(Math.round(v)),
                stats.section().enrolledCount() + " of " + stats.section().capacity() + " seats"));
        cards.add(new StatCard("trend", Theme.series(1), "Class average", stats.averagePercent(), v -> String.format("%.1f%%", v),
                "letter " + GradeScale.letter(stats.averagePercent())));
        cards.add(new StatCard("award", Theme.series(2), "Strongest assessment", best, v -> String.format("%.0f%%", v), bestName));
        cards.add(new StatCard("percent", Theme.series(3), "Average spread", sd, v -> String.format("±%.1f", v),
                "standard deviation, points"));
        col.add(cards);

        JPanel row = new JPanel(new MigLayout("insets 0, fillx, gap 18", "[grow 58, fill][grow 42, fill]", "[top]"));
        row.setOpaque(false);
        List<String> bins = new ArrayList<>();
        double[] counts = new double[10];
        for (int i = 0; i < 10; i++) {
            bins.add(i * 10 + (i == 9 ? "–100" : "–" + (i * 10 + 9)));
            counts[i] = stats.histogram()[i];
        }
        BarChart hist = new BarChart();
        hist.data(bins, "Students", counts, Theme.series(0));
        row.add(Parts.chart("Score distribution", "Students by weighted percentage", hist, 260));
        List<String> letters = new ArrayList<>();
        List<Double> lv = new ArrayList<>();
        List<java.awt.Color> colors = new ArrayList<>();
        stats.letters().forEach((k, v) -> {
            if (v > 0) {
                letters.add(k);
                lv.add((double) v);
                colors.add(Parts.gradeColor(k));
            }
        });
        if (letters.isEmpty()) {
            row.add(Parts.section("Grade distribution", null, new EmptyState("award", "No final grades yet",
                    "Publish final grades from the gradebook to see the split.")));
        } else {
            DonutChart donut = new DonutChart().caption("Graded").colors(colors.toArray(new java.awt.Color[0]));
            donut.data(letters, lv.stream().mapToDouble(d -> d).toArray());
            row.add(Parts.chart("Grade distribution", stats.finalized() + " published", donut, 260));
        }
        col.add(row);

        JPanel row2 = new JPanel(new MigLayout("insets 0, fillx, gap 18", "[grow 60, fill][grow 40, fill]", "[top]"));
        row2.setOpaque(false);
        List<String> names = stats.components().stream().map(InstructorService.ComponentStats::name).toList();
        if (!names.isEmpty()) {
            double[][] vals = new double[3][names.size()];
            for (int i = 0; i < names.size(); i++) {
                var c = stats.components().get(i);
                vals[0][i] = c.minPercent();
                vals[1][i] = c.avgPercent();
                vals[2][i] = c.maxPercent();
            }
            Bar3DChart c3 = new Bar3DChart().max(100).format(v -> String.format("%.0f%%", v));
            c3.data(names, List.of("Lowest", "Average", "Highest"), vals);
            row2.add(Parts.chart("Range by assessment", "Lowest, average and highest score, in 3D", c3, 320));
            if (names.size() >= 3) {
                RadarChart radar = new RadarChart();
                radar.axes(names, 100);
                radar.addSeries(stats.components().stream().mapToDouble(InstructorService.ComponentStats::avgPercent).toArray(),
                        Theme.series(0));
                row2.add(Parts.chart("Average profile", null, radar, 320));
            } else {
                BarChart avg = new BarChart().max(100).format(v -> String.format("%.0f%%", v));
                avg.data(names, "Average", vals[1], Theme.series(1));
                row2.add(Parts.chart("Average by assessment", null, avg, 320));
            }
        }
        col.add(row2);

        DataTable<InstructorService.ComponentStats> t = new DataTable<InstructorService.ComponentStats>()
                .strong("Assessment", 160, InstructorService.ComponentStats::name)
                .num("Out of", 80, c -> fmt(c.maxScore()))
                .num("Weight", 80, c -> fmt(c.weight()) + "%")
                .num("Marked", 80, InstructorService.ComponentStats::graded)
                .progress("Average", 200, c -> c.avgPercent() / 100, c -> String.format("%.1f%%", c.avgPercent()))
                .num("Lowest", 90, c -> String.format("%.0f%%", c.minPercent()))
                .num("Highest", 90, c -> String.format("%.0f%%", c.maxPercent()))
                .num("Std dev", 90, c -> String.format("%.1f", c.stdDevPercent()));
        t.setRows(stats.components());
        JPanel wrap = new JPanel(new MigLayout("insets 0, fill", "[grow, fill]", "[grow, fill]"));
        wrap.setOpaque(false);
        wrap.add(t, "h " + (60 + stats.components().size() * 42) + "!");
        col.add(Parts.section("Assessment statistics", "Percentages of each assessment's maximum", wrap));
        setBody(col, true);
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format("%.1f", v);
    }
}
