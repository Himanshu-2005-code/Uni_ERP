package edu.univ.erp.ui.page.common;

import edu.univ.erp.domain.Announcement;
import edu.univ.erp.domain.Role;
import edu.univ.erp.domain.Section;
import edu.univ.erp.service.AdminService;
import edu.univ.erp.service.InstructorService;
import edu.univ.erp.service.SettingsService;
import edu.univ.erp.ui.kit.Async;
import edu.univ.erp.ui.kit.Avatar;
import edu.univ.erp.ui.kit.Badge;
import edu.univ.erp.ui.kit.Btn;
import edu.univ.erp.ui.kit.Card;
import edu.univ.erp.ui.kit.EmptyState;
import edu.univ.erp.ui.kit.FadeIn;
import edu.univ.erp.ui.kit.Modal;
import edu.univ.erp.ui.kit.Segmented;
import edu.univ.erp.ui.kit.Select;
import edu.univ.erp.ui.kit.Toasts;
import edu.univ.erp.ui.kit.UI;
import edu.univ.erp.ui.page.Page;
import edu.univ.erp.ui.page.Parts;
import edu.univ.erp.ui.page.Shell;
import edu.univ.erp.ui.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.util.List;

/** Campus and course notices; instructors and admins can post. */
public class NoticesPage extends Page {

    private List<Announcement> notices = List.of();
    private final Segmented filter = new Segmented("All", "Campus", "Courses");
    private final JPanel list = new JPanel(new MigLayout("insets 0, fillx, wrap 1, gap 14", "[grow, fill]", ""));

    public NoticesPage(Shell shell) {
        super(shell);
        list.setOpaque(false);
        filter.onChange(i -> renderList());
    }

    @Override
    public String title() {
        return "Notices";
    }

    @Override
    public String subtitle() {
        return "Announcements from " + SettingsService.get().institution() + " and your courses";
    }

    @Override
    public void reload() {
        load(shell::loadNotices, n -> {
            notices = n;
            shell.markNoticesSeen();
            JPanel col = column();
            JPanel bar = new JPanel(new MigLayout("insets 0, fillx", "[]push[]", "[center]"));
            bar.setOpaque(false);
            bar.add(filter, "h 38!");
            if (shell.user().role() != Role.STUDENT) {
                Btn post = Btn.primary("New notice", "megaphone");
                post.addActionListener(e -> compose());
                bar.add(post);
            }
            col.add(bar);
            col.add(list);
            setBody(col, true);
            renderList();
        });
    }

    private void renderList() {
        list.removeAll();
        int i = 0;
        for (Announcement a : notices) {
            if (filter.selected() == 1 && !a.isCampusWide()) continue;
            if (filter.selected() == 2 && a.isCampusWide()) continue;
            list.add(FadeIn.of(card(a), i++));
        }
        if (i == 0) list.add(new EmptyState("megaphone", "Nothing here yet", "New notices will show up here."));
        list.revalidate();
        list.repaint();
    }

    private JComponent card(Announcement a) {
        Card c = new Card(new MigLayout("insets 0, fillx", "[]14[grow]push[]", "[]2[]10[]"));
        c.hoverLift();
        c.add(new JLabel(new Avatar(a.authorName(), 42)), "spany 3, aligny top");
        c.add(UI.h2(UI.esc(a.title())), "wmin 0");
        boolean canDelete = shell.user().role() == Role.ADMIN || a.authorId() == shell.user().id();
        if (canDelete) {
            Btn del = Btn.icon("trash", "Delete notice");
            del.addActionListener(e -> Modal.confirm(this, "Delete this notice?", "\"" + UI.esc(a.title())
                    + "\" will be removed for everyone.", "Delete", true, () -> Async.run(this, () -> {
                        if (shell.user().role() == Role.ADMIN) AdminService.deleteAnnouncement(a.id());
                        else InstructorService.deleteAnnouncement(a.id());
                        return true;
                    }, ok -> {
                        Toasts.success(this, "Notice deleted.");
                        reload();
                    })));
            c.add(del, "spany 2, aligny top, wrap");
        } else {
            c.add(new JLabel(), "wrap");
        }
        JPanel meta = new JPanel(new MigLayout("insets 0, gap 8", "", "[center]"));
        meta.setOpaque(false);
        meta.add(UI.muted(a.authorName() + "  ·  " + Shell.ago(a.createdAt())));
        meta.add(new Badge(a.isCampusWide() ? "Campus-wide" : a.sectionLabel(), a.isCampusWide() ? Theme.series(5)
                : Theme.series(1)));
        c.add(meta, "cell 1 1, span 2");
        c.add(UI.html("<div style='width:640px;line-height:1.45'>" + UI.esc(a.body()).replace("\n", "<br>") + "</div>",
                13.5f, false), "cell 1 2, span 2, wmin 0");
        return c;
    }

    private void compose() {
        boolean admin = shell.user().role() == Role.ADMIN;
        Modal m = new Modal(this, "New notice", admin ? "Sent to everyone on campus." : "Sent to students in one of your sections.",
                "megaphone").width(560);
        JPanel form = new JPanel(new MigLayout("insets 0, fillx, wrap 1, gapy 14", "[grow, fill]", ""));
        form.setOpaque(false);
        Select<Section> section = new Select<Section>().placeholder("Choose a section").labels(Section::label);
        if (!admin) {
            String term = SettingsService.get().term();
            Async.run(this, InstructorService::sections, secs -> {
                List<Section> current = secs.stream().filter(s -> s.term().equals(term)).toList();
                section.options(current.isEmpty() ? secs : current);
                if (!section.options().isEmpty()) section.select(section.options().get(0));
            });
            form.add(UI.labeled("Section", section, null));
        }
        JTextField title = UI.field("A short headline", null);
        form.add(UI.labeled("Title", title, null));
        JTextArea body = UI.area(6);
        form.add(UI.labeledArea("Message", UI.areaScroll(body)), "h 170!");
        m.content(form);
        m.cancel("Cancel");
        Btn post = Btn.primary("Post notice", "megaphone");
        post.addActionListener(e -> Async.button(post, () -> {
            if (admin) AdminService.postAnnouncement(title.getText(), body.getText());
            else {
                if (section.selected() == null) throw new edu.univ.erp.service.ServiceException("Choose a section.");
                InstructorService.postAnnouncement(section.selected().id(), title.getText(), body.getText());
            }
            return true;
        }, ok -> {
            m.close();
            Toasts.success(this, "Notice posted.");
            reload();
            shell.pollStatus();
        }));
        m.primary(post);
        m.show();
    }

    /** A short list for dashboards. */
    public static JComponent compactList(List<Announcement> notices, int max) {
        JPanel p = new JPanel(new MigLayout("insets 0, fillx, wrap 1, gapy 12", "[grow, fill]", ""));
        p.setOpaque(false);
        if (notices.isEmpty()) {
            p.add(UI.muted("No notices yet."));
            return p;
        }
        int n = 0;
        for (Announcement a : notices) {
            if (n++ == max) break;
            JPanel r = new JPanel(new MigLayout("insets 0, fillx", "[]12[grow]", "[]0[]"));
            r.setOpaque(false);
            r.add(Parts.bubble(a.isCampusWide() ? "megaphone" : "book", a.isCampusWide() ? Theme.series(5) : Theme.series(1), 34),
                    "spany 2, aligny top");
            JLabel t = UI.h3(UI.esc(a.title()));
            r.add(t, "wmin 0, wrap");
            r.add(UI.muted((a.isCampusWide() ? "Campus" : a.sectionLabel()) + " · " + Shell.ago(a.createdAt())), "cell 1 1, wmin 0");
            p.add(r);
        }
        return p;
    }
}
