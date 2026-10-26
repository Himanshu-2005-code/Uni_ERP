package edu.univ.erp.ui.page.student;

import edu.univ.erp.domain.GradeComponent;
import edu.univ.erp.domain.GradeEntry;
import edu.univ.erp.service.ExportService;
import edu.univ.erp.service.StudentService;
import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;
import edu.univ.erp.ui.chart.Gauge;
import edu.univ.erp.ui.chart.RadarChart;
import edu.univ.erp.ui.kit.Async;
import edu.univ.erp.ui.kit.Badge;
import edu.univ.erp.ui.kit.Btn;
import edu.univ.erp.ui.kit.Card;
import edu.univ.erp.ui.kit.DropdownMenu;
import edu.univ.erp.ui.kit.EmptyState;
import edu.univ.erp.ui.kit.FadeIn;
import edu.univ.erp.ui.kit.Icons;
import edu.univ.erp.ui.kit.Toasts;
import edu.univ.erp.ui.kit.UI;
import edu.univ.erp.ui.page.Page;
import edu.univ.erp.ui.page.Parts;
import edu.univ.erp.ui.page.Shell;
import edu.univ.erp.ui.theme.Theme;
import edu.univ.erp.util.GradeScale;
import net.miginfocom.swing.MigLayout;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** GPA, per-course assessment breakdowns and transcript exports. */
public class GradesPage extends Page {

    private record Data(List<StudentService.CourseResult> results, StudentService.GpaSummary gpa) {
    }

    public GradesPage(Shell shell) {
        super(shell);
    }

    @Override
    public String title() {
        return "Grades & transcript";
    }

    @Override
    public String subtitle() {
        return "Every assessment, every term";
    }

    @Override
    public void reload() {
        load(() -> new Data(StudentService.results(), StudentService.gpa()), this::render);
    }

    private void render(Data d) {
        JPanel col = column();
        JPanel top = new JPanel(new MigLayout("insets 0, fillx, gap 18", "[300!, fill][grow, fill]", "[top]"));
        top.setOpaque(false);

        Gauge g = new Gauge().set(d.gpa().cgpa(), 10, String.format("%.2f", d.gpa().cgpa()), "CGPA");
        JPanel gp = new JPanel(new MigLayout("insets 0, fillx, wrap 1", "[grow, fill]", "[]10[]"));
        gp.setOpaque(false);
        gp.add(g, "h 190!");
        gp.add(Parts.facts(new String[][]{{"Credits earned", String.valueOf(d.gpa().creditsEarned())},
                {"Credits graded", String.valueOf(d.gpa().creditsGraded())},
                {"Terms completed", String.valueOf(d.gpa().termGpa().size())}}));
        Btn export = Btn.primary("Export", "download");
        export.addActionListener(e -> exportMenu(export));
        top.add(Parts.section("Overall", null, gp, export));

        List<StudentService.CourseResult> inProgress = d.results().stream()
                .filter(r -> !r.enrollment().hasFinal() && r.runningPercent() != null).toList();
        if (inProgress.size() >= 3) {
            RadarChart radar = new RadarChart();
            radar.axes(inProgress.stream().map(r -> r.enrollment().section().courseCode()).toList(), 100);
            radar.addSeries(inProgress.stream().mapToDouble(StudentService.CourseResult::runningPercent).toArray(),
                    Theme.series(0));
            top.add(Parts.chart("This term's shape", "Weighted score so far in each course", radar, 300));
        } else {
            top.add(Parts.section("This term's shape", null, new EmptyState("pie", "Not enough marks yet",
                    "The radar appears once three or more courses have marks.")));
        }
        col.add(top);

        if (d.results().isEmpty()) {
            col.add(new EmptyState("award", "No grades yet", "Marks appear here as soon as your instructors record them."));
        } else {
            String term = null;
            int i = 0;
            for (StudentService.CourseResult r : d.results()) {
                if (!r.enrollment().section().term().equals(term)) {
                    term = r.enrollment().section().term();
                    Double tg = d.gpa().termGpa().get(term);
                    JPanel h = new JPanel(new MigLayout("insets 10 4 0 4, fillx", "[]push[]", ""));
                    h.setOpaque(false);
                    h.add(UI.h2(term));
                    if (tg != null) h.add(new Badge(String.format("Term GPA %.2f", tg), Theme.series(0)));
                    col.add(h);
                }
                col.add(FadeIn.of(new CourseRow(r, i == 0), i++));
            }
        }
        setBody(col, true);
    }

    private void exportMenu(Btn anchor) {
        DropdownMenu m = new DropdownMenu().width(260);
        m.add(DropdownMenu.Item.rich("file", "Transcript (PDF)", "Formatted, ready to print or share", () ->
                save("transcript.pdf", ExportService::transcriptPdf)));
        m.add(DropdownMenu.Item.rich("table", "Transcript (CSV)", "Courses and grades for spreadsheets", () ->
                save("transcript.csv", ExportService::transcriptCsv)));
        m.add(DropdownMenu.Item.rich("bars", "Assessment scores (CSV)", "Every mark in every course", () ->
                save("grade-report.csv", ExportService::gradeReportCsv)));
        m.show(anchor, DropdownMenu.Align.RIGHT);
    }

    interface Exporter {
        void write(Path p) throws Exception;
    }

    private void save(String name, Exporter ex) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(System.getProperty("user.home"), shell.user().username() + "-" + name));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path p = fc.getSelectedFile().toPath();
        Async.run(this, () -> {
            try {
                ex.write(p);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            return p;
        }, path -> {
            Toasts.success(this, "Saved " + path.getFileName() + ".");
            try {
                if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().open(path.toFile());
            } catch (Exception ignored) {
                // no default app for the file type
            }
        });
    }

    /** One course: header always visible, assessment breakdown unfolds on click. */
    private static final class CourseRow extends Card {
        private final JPanel detail;
        private double open;
        private boolean expanded;
        private final ChevronIcon chevron = new ChevronIcon();
        private Animator.Anim anim;

        CourseRow(StudentService.CourseResult r, boolean startOpen) {
            super(new MigLayout("insets 0, fillx, wrap 1, hidemode 3", "[grow, fill]", "[]0[]"));
            hoverLift();
            pad(14, 20, 14, 20);
            var e = r.enrollment();
            JPanel head = new JPanel(new MigLayout("insets 0, fillx", "[]14[grow]16[140!]16[]10[]", "[]2[]"));
            head.setOpaque(false);
            head.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            head.add(new JLabel(new edu.univ.erp.ui.kit.Avatar(e.section().courseCode(), 40)), "spany 2");
            head.add(UI.h3(e.section().courseCode() + " · " + e.section().courseTitle()), "wmin 0");
            Double pct = e.finalPercentage() != null ? e.finalPercentage() : r.runningPercent();
            head.add(Parts.bar(pct == null ? 0 : pct / 100, pct == null ? Theme.p().subtle() : Parts.percentColor(pct)),
                    "spany 2, h 8!");
            JLabel pl = new UI.ThemedLabel(pct == null ? "—" : String.format("%.1f%%", pct), false);
            pl.setFont(Theme.semibold(14f));
            head.add(pl, "spany 2");
            String letter = e.hasFinal() ? e.finalGrade() : pct == null ? "—" : "IP";
            head.add(new Badge(e.hasFinal() ? letter + " · " + (int) GradeScale.points(letter) + " pts" : "In progress",
                    e.hasFinal() ? Parts.gradeColor(letter) : Theme.p().muted()), "spany 2, wrap");
            head.add(UI.muted(e.section().credits() + " credits · " + (e.section().instructorName() == null ? "TBA"
                    : e.section().instructorName())), "cell 1 1, wmin 0");
            JLabel chev = new JLabel(chevron);
            head.add(chev, "cell 5 0, spany 2");
            add(head);

            detail = new JPanel(new MigLayout("insets 14 54 4 0, fillx, wrap 5", "[grow 40, fill][90!][160!, fill][70!][70!]", "[]8[]")) {
                @Override
                public Dimension getPreferredSize() {
                    Dimension d = super.getPreferredSize();
                    return new Dimension(d.width, (int) (d.height * open));
                }
            };
            detail.setOpaque(false);
            for (String h : new String[]{"Assessment", "Score", "", "Percent", "Weight"}) {
                JLabel l = UI.muted(h.toUpperCase());
                l.setFont(Theme.semibold(10.5f));
                detail.add(l);
            }
            List<GradeComponent> comps = new ArrayList<>(r.components());
            for (GradeComponent c : comps) {
                GradeEntry g = r.scores().get(c.name());
                detail.add(UI.text(c.name(), 13f));
                detail.add(UI.text(g == null ? "—" : fmt(g.score()) + " / " + fmt(c.maxScore()), 13f));
                detail.add(Parts.bar(g == null ? 0 : g.percent() / 100, g == null ? Theme.p().subtle()
                        : Parts.percentColor(g.percent())), "h 8!");
                detail.add(UI.text(g == null ? "—" : String.format("%.0f%%", g.percent()), 13f));
                detail.add(UI.muted(fmt(c.weight()) + "%"));
            }
            if (comps.isEmpty()) detail.add(UI.muted("No assessments defined yet."), "span 5");
            add(detail);
            MouseAdapter toggle = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent ev) {
                    toggle();
                }
            };
            head.addMouseListener(toggle);
            trackChildHover(head);
            if (startOpen) {
                expanded = true;
                open = 1;
                chevron.turn = 1;
            }
            detail.setVisible(open > 0);
        }

        private void toggle() {
            expanded = !expanded;
            if (anim != null) anim.cancel();
            double from = open;
            double to = expanded ? 1 : 0;
            detail.setVisible(true);
            anim = Animator.animate(380, Easing.IN_OUT_CUBIC, t -> {
                open = Animator.lerp(from, to, t);
                chevron.turn = open;
                detail.revalidate();
                revalidate();
                repaint();
            }, () -> detail.setVisible(expanded));
        }

        private static String fmt(double v) {
            return v == Math.rint(v) ? String.valueOf((long) v) : String.format("%.1f", v);
        }
    }

    private static final class ChevronIcon implements javax.swing.Icon {
        double turn;

        @Override
        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            java.awt.Graphics2D g2 = Theme.hq(g);
            Icons.paintRotated(g2, "chevron-down", x + 10, y + 10, 18, Math.PI * turn, Theme.p().muted(), 2f);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 20;
        }

        @Override
        public int getIconHeight() {
            return 20;
        }
    }
}
