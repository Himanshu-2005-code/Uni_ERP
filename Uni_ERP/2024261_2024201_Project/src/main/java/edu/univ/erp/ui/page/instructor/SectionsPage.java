package edu.univ.erp.ui.page.instructor;

import edu.univ.erp.domain.Section;
import edu.univ.erp.service.InstructorService;
import edu.univ.erp.service.SettingsService;
import edu.univ.erp.ui.chart.Gauge;
import edu.univ.erp.ui.kit.Avatar;
import edu.univ.erp.ui.kit.Badge;
import edu.univ.erp.ui.kit.Btn;
import edu.univ.erp.ui.kit.Card;
import edu.univ.erp.ui.kit.EmptyState;
import edu.univ.erp.ui.kit.FadeIn;
import edu.univ.erp.ui.kit.Segmented;
import edu.univ.erp.ui.kit.UI;
import edu.univ.erp.ui.page.Page;
import edu.univ.erp.ui.page.Shell;
import edu.univ.erp.ui.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.List;

/** All sections the instructor teaches, as cards. */
public class SectionsPage extends Page {

    private List<Section> sections = List.of();
    private final Segmented scope = new Segmented("This term", "All terms");
    private final JPanel grid = new JPanel(new MigLayout("insets 0, fillx, wrap 3, gap 18", "[grow, fill, sg c]".repeat(3), ""));

    public SectionsPage(Shell shell) {
        super(shell);
        grid.setOpaque(false);
        scope.onChange(i -> renderGrid());
    }

    @Override
    public String title() {
        return "My sections";
    }

    @Override
    public String subtitle() {
        return sections.size() + " sections taught";
    }

    @Override
    public void reload() {
        load(InstructorService::sections, s -> {
            sections = Selection.order(s);
            shell.refreshTitle(this);
            JPanel col = column();
            col.add(scope, "w pref!, h 38!");
            col.add(grid);
            setBody(col, true);
            renderGrid();
        });
    }

    private void renderGrid() {
        grid.removeAll();
        String term = SettingsService.get().term();
        int i = 0;
        for (Section s : sections) {
            if (scope.selected() == 0 && !s.term().equals(term)) continue;
            grid.add(FadeIn.of(card(s, s.term().equals(term)), i++));
        }
        if (i == 0) grid.add(new EmptyState("layers", "No sections", "Nothing scheduled for this term."), "span 3");
        grid.revalidate();
        grid.repaint();
    }

    private JComponent card(Section s, boolean current) {
        Card c = new Card(new MigLayout("insets 0, fillx, wrap 2", "[grow][]", "[]6[]4[]4[]16[]"));
        c.hoverLift();
        JPanel head = new JPanel(new MigLayout("insets 0, gap 10", "[][]", "[center]"));
        head.setOpaque(false);
        head.add(new JLabel(new Avatar(s.courseCode(), 38)));
        head.add(UI.h3(s.courseCode()));
        c.add(head);
        c.add(new Badge(s.term(), current ? Theme.p().success() : Theme.p().muted()).dot(), "aligny top");
        c.add(UI.h2("<html><div style='width:162px'>" + UI.esc(s.courseTitle()) + "</div></html>"), "span 2");
        Gauge g = new Gauge().set(s.enrolledCount(), s.capacity(), String.valueOf(s.enrolledCount()), "of " + s.capacity());
        c.add(UI.muted(s.dayTime()), "span 1");
        c.add(g, "spany 2, w 96!, h 96!, aligny top");
        c.add(UI.muted("Room " + s.room() + "  ·  " + s.credits() + " credits"));
        JPanel foot = new JPanel(new MigLayout("insets 0, fillx, gap 8", "[grow, fill][grow, fill]", ""));
        foot.setOpaque(false);
        Btn gb = Btn.primary("Gradebook", "clipboard").compact();
        gb.addActionListener(e -> {
            Selection.sectionId = s.id();
            shell.go("gradebook");
        });
        Btn an = Btn.secondary("Analytics", "bars").compact();
        an.addActionListener(e -> {
            Selection.sectionId = s.id();
            shell.go("analytics");
        });
        foot.add(gb);
        foot.add(an);
        c.add(foot, "span 2, growx");
        return c;
    }
}
