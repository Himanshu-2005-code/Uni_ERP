package edu.univ.erp.ui.page.student;

import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.service.SettingsService;
import edu.univ.erp.service.StudentService;
import edu.univ.erp.ui.kit.Async;
import edu.univ.erp.ui.kit.Avatar;
import edu.univ.erp.ui.kit.Badge;
import edu.univ.erp.ui.kit.Btn;
import edu.univ.erp.ui.kit.Card;
import edu.univ.erp.ui.kit.DataTable;
import edu.univ.erp.ui.kit.EmptyState;
import edu.univ.erp.ui.kit.FadeIn;
import edu.univ.erp.ui.kit.Modal;
import edu.univ.erp.ui.kit.Toasts;
import edu.univ.erp.ui.kit.UI;
import edu.univ.erp.ui.page.Page;
import edu.univ.erp.ui.page.Parts;
import edu.univ.erp.ui.page.Shell;
import edu.univ.erp.ui.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Current registrations (with drop) and the full registration history. */
public class MyCoursesPage extends Page {

    private record Data(List<Enrollment> all, List<Enrollment> current, Map<Integer, Double> running,
                        LocalDate dropBy, LocalDate addBy, String term) {
    }

    public MyCoursesPage(Shell shell) {
        super(shell);
    }

    @Override
    public String title() {
        return "My courses";
    }

    @Override
    public String subtitle() {
        return "Registrations and history";
    }

    @Override
    public void reload() {
        load(() -> {
            SettingsService s = SettingsService.get().refresh();
            Map<Integer, Double> running = StudentService.results().stream()
                    .filter(r -> r.runningPercent() != null)
                    .collect(Collectors.toMap(r -> r.enrollment().id(), StudentService.CourseResult::runningPercent));
            return new Data(StudentService.enrollments(), StudentService.currentRegistrations(), running,
                    s.dropDeadline(), s.addDeadline(), s.term());
        }, this::render);
    }

    private void render(Data d) {
        JPanel col = column();
        boolean dropOpen = d.dropBy() == null || !LocalDate.now().isAfter(d.dropBy());
        int credits = d.current().stream().mapToInt(e -> e.section().credits()).sum();
        col.add(Parts.banner(dropOpen ? "calendar" : "alert", dropOpen ? Theme.p().accent() : Theme.p().warning(),
                d.term() + ": <b>" + d.current().size() + " courses, " + credits + " credits</b>. "
                        + (d.addBy() != null ? "Add by " + Parts.date(d.addBy()) + ". " : "")
                        + (d.dropBy() != null ? (dropOpen ? "You can drop until " : "The drop deadline passed on ")
                        + Parts.date(d.dropBy()) + "." : "")));

        if (d.current().isEmpty()) {
            Btn browse = Btn.primary("Browse catalog", "book");
            browse.addActionListener(e -> shell.go("catalog"));
            JPanel empty = new JPanel(new MigLayout("insets 0, wrap 1, alignx center", "[center]", ""));
            empty.setOpaque(false);
            empty.add(new EmptyState("layers", "No courses this term", "Pick sections from the catalog to build your timetable."));
            empty.add(browse);
            col.add(Parts.section("This term", null, empty));
        } else {
            JPanel list = new JPanel(new MigLayout("insets 0, fillx, wrap 2, gap 18", "[grow, fill, sg a][grow, fill, sg a]", ""));
            list.setOpaque(false);
            int i = 0;
            for (Enrollment e : d.current()) list.add(FadeIn.of(courseCard(e, d.running().get(e.id()), dropOpen), i++));
            col.add(list);
        }

        DataTable<Enrollment> hist = new DataTable<Enrollment>()
                .col("Term", 130, e -> e.section().term())
                .strong("Code", 90, e -> e.section().courseCode())
                .col("Course", 260, e -> e.section().courseTitle())
                .num("Credits", 70, e -> e.section().credits())
                .col("Instructor", 180, e -> e.section().instructorName())
                .badge("Status", 130, e -> cap(e.status()), Parts::statusColor)
                .badge("Grade", 90, e -> e.hasFinal() ? e.finalGrade() : "", v -> Parts.gradeColor(String.valueOf(v)))
                .search(e -> e.section().courseCode() + " " + e.section().courseTitle() + " " + e.section().term())
                .empty("history", "No history yet", null);
        hist.setRows(d.all());
        JPanel wrap = new JPanel(new MigLayout("insets 0, fill", "[grow, fill]", "[grow, fill]"));
        wrap.setOpaque(false);
        wrap.add(hist, "h " + Math.min(460, 60 + d.all().size() * 42) + "!");
        col.add(Parts.section("Registration history", d.all().size() + " records across all terms", wrap));
        setBody(col, true);
    }

    private static String cap(String s) {
        return s.charAt(0) + s.substring(1).toLowerCase();
    }

    private Card courseCard(Enrollment e, Double running, boolean dropOpen) {
        Card c = new Card(new MigLayout("insets 0, fillx", "[]14[grow]push[]", "[]4[]4[]"));
        c.hoverLift();
        c.add(new JLabel(new Avatar(e.section().courseCode(), 46)), "spany 3, aligny top");
        c.add(UI.h2(e.section().courseCode() + " · " + e.section().courseTitle()), "wmin 0, growx");
        c.add(new Badge(e.section().credits() + " cr", Theme.p().muted()), "wrap");
        c.add(UI.muted((e.section().instructorName() == null ? "TBA" : e.section().instructorName()) + "  ·  "
                + e.section().dayTime() + "  ·  " + e.section().room()), "cell 1 1, span 2, wmin 0, wrap");
        JPanel bottom = new JPanel(new MigLayout("insets 0, fillx", "[grow, fill]12[]12[]", "[center]"));
        bottom.setOpaque(false);
        double pct = running == null ? 0 : running;
        bottom.add(Parts.bar(pct / 100, running == null ? Theme.p().subtle() : Parts.percentColor(pct)), "h 8!");
        JLabel score = new UI.ThemedLabel(running == null ? "Not graded yet" : String.format("%.1f%% so far", pct), true);
        score.setFont(Theme.medium(12f));
        bottom.add(score);
        Btn drop = Btn.ghost("Drop", "minus").compact();
        drop.putClientProperty("fg", Theme.p().danger());
        drop.setEnabled(dropOpen && !e.hasFinal() && !shell.maintenance());
        drop.addActionListener(ev -> Modal.confirm(this, "Drop " + e.section().courseCode() + "?",
                "You'll lose your seat in <b>" + UI.esc(e.section().courseTitle()) + "</b> and any marks recorded so far. "
                        + "You can register again while seats are available.",
                "Drop course", true, () -> Async.run(this, () -> {
                    StudentService.drop(e.id());
                    return true;
                }, ok -> {
                    Toasts.success(this, "Dropped " + e.section().courseCode() + ".");
                    reload();
                })));
        bottom.add(drop);
        c.add(bottom, "cell 1 2, span 2, growx");
        return c;
    }
}
