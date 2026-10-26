package edu.univ.erp.ui.page.student;

import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.domain.Section;
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
import edu.univ.erp.ui.kit.Icons;
import edu.univ.erp.ui.kit.Modal;
import edu.univ.erp.ui.kit.Segmented;
import edu.univ.erp.ui.kit.Select;
import edu.univ.erp.ui.kit.Toasts;
import edu.univ.erp.ui.kit.Toggle;
import edu.univ.erp.ui.kit.UI;
import edu.univ.erp.ui.page.Page;
import edu.univ.erp.ui.page.Parts;
import edu.univ.erp.ui.page.Shell;
import edu.univ.erp.ui.theme.Theme;
import edu.univ.erp.util.TimeSlot;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Browse this term's sections and register, with clashes and seat limits flagged up front. */
public class CatalogPage extends Page {

    private record Data(List<Section> sections, List<Enrollment> mine, int maxCredits, String term) {
    }

    private Data data;
    private final JTextField search = UI.search("Search code, title or instructor");
    private final Select<String> dept = new Select<String>().placeholder("All departments").leading("filter");
    private final Segmented view = new Segmented("Cards", "Table");
    private final Toggle openOnly = new Toggle(false);
    private final JPanel results = new JPanel(new MigLayout("insets 0, fill", "[grow, fill]", "[grow, fill]"));

    public CatalogPage(Shell shell) {
        super(shell);
        results.setOpaque(false);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                renderResults();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                renderResults();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        dept.onChange(d -> renderResults());
        view.onChange(i -> renderResults());
        openOnly.onChange(b -> renderResults());
    }

    @Override
    public String title() {
        return "Course catalog";
    }

    @Override
    public String subtitle() {
        return data == null ? "Sections open for registration" : data.term() + " · " + data.sections().size() + " sections";
    }

    @Override
    public void reload() {
        load(() -> {
            SettingsService s = SettingsService.get().refresh();
            return new Data(StudentService.catalog(), StudentService.currentRegistrations(), s.maxCredits(), s.term());
        }, d -> {
            data = d;
            shell.refreshTitle(this);
            TreeSet<String> depts = new TreeSet<>();
            for (Section s : d.sections()) if (s.department() != null) depts.add(s.department());
            List<String> opts = new ArrayList<>();
            opts.add("All departments");
            opts.addAll(depts);
            String keep = dept.selected();
            dept.options(opts);
            dept.setSelectedQuietly(keep == null ? "All departments" : keep);
            build();
        });
    }

    private void build() {
        JPanel col = column();
        int credits = data.mine().stream().mapToInt(e -> e.section().credits()).sum();
        JPanel bar = new JPanel(new MigLayout("insets 0, fillx", "[grow, fill]12[220!]12[]8[]push[]", "[center]"));
        bar.setOpaque(false);
        bar.add(search, "h 40!");
        bar.add(dept, "h 40!");
        bar.add(openOnly);
        bar.add(UI.muted("Open seats only"));
        bar.add(view, "h 38!");
        col.add(bar);
        col.add(Parts.banner("info", Theme.p().accent(), "You have <b>" + credits + " of " + data.maxCredits()
                + "</b> credits this term. Sections that clash with your timetable are marked before you register."));
        col.add(results, "grow, push");
        setBody(col, true);
        renderResults();
    }

    /** What a section means for this student right now. */
    private String status(Section s) {
        for (Enrollment e : data.mine()) {
            if (e.sectionId() == s.id()) return "Registered";
        }
        for (Enrollment e : data.mine()) {
            if (e.section().courseId() == s.courseId()) return "In other section";
            if (TimeSlot.clash(e.section().dayTime(), s.dayTime())) return "Clash: " + e.section().courseCode();
        }
        if (s.isFull()) return "Full";
        int credits = data.mine().stream().mapToInt(e -> e.section().credits()).sum();
        if (credits + s.credits() > data.maxCredits()) return "Over credit limit";
        return "Open";
    }

    private static Color statusColor(String s) {
        if (s.equals("Registered")) return Theme.p().success();
        if (s.equals("Open")) return Theme.series(0);
        if (s.startsWith("Clash") || s.equals("Full")) return Theme.p().danger();
        return Theme.p().warning();
    }

    private List<Section> filtered() {
        String q = search.getText().trim().toLowerCase();
        String d = dept.selected();
        List<Section> out = new ArrayList<>();
        for (Section s : data.sections()) {
            if (d != null && !d.equals("All departments") && !d.equals(s.department())) continue;
            if (openOnly.isOn() && s.isFull()) continue;
            String hay = (s.courseCode() + " " + s.courseTitle() + " " + s.instructorName() + " " + s.room()).toLowerCase();
            if (!q.isEmpty() && !hay.contains(q)) continue;
            out.add(s);
        }
        return out;
    }

    private void renderResults() {
        if (data == null) return;
        results.removeAll();
        List<Section> list = filtered();
        if (list.isEmpty()) {
            results.add(new EmptyState("search", "No sections match", "Try a different search or department."));
        } else if (view.selected() == 0) {
            JPanel grid = new JPanel(new MigLayout("insets 0, fillx, wrap 3, gap 18", "[grow, fill, sg c]".repeat(3), ""));
            grid.setOpaque(false);
            for (int i = 0; i < list.size(); i++) grid.add(FadeIn.of(card(list.get(i)), i));
            results.add(grid, "growx, aligny top");
        } else {
            DataTable<Section> t = new DataTable<Section>()
                    .strong("Code", 90, Section::courseCode)
                    .col("Course", 230, Section::courseTitle)
                    .num("Credits", 70, Section::credits)
                    .person("Instructor", 200, s -> s.instructorName() == null ? "TBA" : s.instructorName(), s -> s.department())
                    .col("Schedule", 170, Section::dayTime)
                    .col("Room", 90, Section::room)
                    .progress("Seats", 170, Section::fillRatio, s -> s.seatsLeft() + " left")
                    .badge("Status", 150, this::status, v -> statusColor(String.valueOf(v)))
                    .onDoubleClick(this::confirmRegister)
                    .menu(s -> List.of(edu.univ.erp.ui.kit.DropdownMenu.Item.of("plus", "Register…", () -> confirmRegister(s))));
            t.setRows(list);
            Card c = new Card(new MigLayout("insets 0, fill", "[grow, fill]", "[grow, fill]"));
            c.pad(6, 6, 6, 6);
            c.add(t, "h 560!");
            results.add(c, "grow");
        }
        results.revalidate();
        results.repaint();
    }

    private JComponent card(Section s) {
        String st = status(s);
        Card c = new Card(new MigLayout("insets 0, fillx, wrap 1", "[grow, fill]", "[]4[]12[]6[]6[]14[]4[]16[]"));
        c.hoverLift();
        JPanel head = new JPanel(new MigLayout("insets 0, fillx", "[]push[]", "[center]"));
        head.setOpaque(false);
        JLabel code = new UI.ThemedLabel(s.courseCode(), false);
        code.setFont(Theme.semibold(13f));
        code.setForeground(Theme.p().accent());
        head.add(new CodeChip(s.courseCode()));
        head.add(new Badge(s.credits() + " credits", Theme.p().muted()));
        c.add(head);
        JLabel title = UI.h2("<html><div style='width:220px'>" + UI.esc(s.courseTitle()) + "</div></html>");
        c.add(title);
        JPanel who = new JPanel(new MigLayout("insets 0, gap 8", "[][grow]", "[center]"));
        who.setOpaque(false);
        who.add(new JLabel(new Avatar(s.instructorName() == null ? "TBA" : s.instructorName(), 24)));
        who.add(UI.muted(s.instructorName() == null ? "Instructor TBA" : s.instructorName()));
        c.add(who);
        c.add(line("clock", s.dayTime()));
        c.add(line("pin", s.room() + (s.department() == null ? "" : "  ·  " + s.department())));
        JPanel seats = new JPanel(new MigLayout("insets 0, fillx", "[grow]push[]", ""));
        seats.setOpaque(false);
        seats.add(UI.muted("Seats"));
        seats.add(UI.muted(s.enrolledCount() + " / " + s.capacity()));
        c.add(seats);
        Color barColor = s.fillRatio() >= 1 ? Theme.p().danger() : s.fillRatio() >= 0.85 ? Theme.p().warning() : Theme.p().accent();
        c.add(Parts.bar(s.fillRatio(), barColor), "h 8!");
        JPanel foot = new JPanel(new MigLayout("insets 0, fillx", "[]push[]", "[center]"));
        foot.setOpaque(false);
        foot.add(new Badge(st, statusColor(st)).dot());
        if (st.equals("Registered")) {
            Btn go = Btn.ghost("View", "arrow-right").compact();
            go.addActionListener(e -> shell.go("courses"));
            foot.add(go);
        } else {
            Btn reg = Btn.primary("Register", "plus").compact();
            reg.setEnabled(st.equals("Open") && !shell.maintenance());
            reg.addActionListener(e -> confirmRegister(s));
            foot.add(reg);
        }
        c.add(foot);
        return c;
    }

    private static JComponent line(String icon, String text) {
        JPanel p = new JPanel(new MigLayout("insets 0, gap 8", "[][grow]", "[center]"));
        p.setOpaque(false);
        p.add(new JLabel(Icons.of(icon, 15, () -> Theme.p().muted())));
        JLabel l = new UI.ThemedLabel(text, false);
        l.setFont(Theme.font(12.5f));
        p.add(l);
        return p;
    }

    private void confirmRegister(Section s) {
        String st = status(s);
        if (!st.equals("Open")) {
            Toasts.warning(this, s.courseCode() + ": " + st + ".");
            return;
        }
        Modal.confirm(this, "Register for " + s.courseCode() + "?",
                "<b>" + UI.esc(s.courseTitle()) + "</b><br>" + s.dayTime() + " · " + s.room() + "<br>"
                        + (s.instructorName() == null ? "" : UI.esc(s.instructorName()) + " · ") + s.credits() + " credits",
                "Register", false, () -> Async.run(this, () -> StudentService.register(s.id()), e -> {
                    Toasts.success(this, "You're registered for " + s.courseCode() + ". See you in " + s.room() + "!");
                    reload();
                }));
    }

    /** The course code in a tinted pill. */
    private static final class CodeChip extends JComponent {
        private final String code;

        CodeChip(String code) {
            this.code = code;
            setFont(Theme.semibold(12f));
        }

        @Override
        public java.awt.Dimension getPreferredSize() {
            return new java.awt.Dimension(getFontMetrics(getFont()).stringWidth(code) + 20, 24);
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            java.awt.Graphics2D g2 = Theme.hq(g);
            Color c = Avatar.colorFor(code);
            g2.setColor(Theme.alpha(c, Theme.isDark() ? 0.22 : 0.12));
            g2.fill(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), 24, 10, 10));
            g2.setFont(getFont());
            g2.setColor(Theme.isDark() ? Theme.brighter(c, 0.3) : Theme.darker(c, 0.1));
            java.awt.FontMetrics fm = g2.getFontMetrics();
            g2.drawString(code, 10, (24 - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }
}
