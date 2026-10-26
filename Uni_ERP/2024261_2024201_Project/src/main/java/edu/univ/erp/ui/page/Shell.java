package edu.univ.erp.ui.page;

import edu.univ.erp.domain.Announcement;
import edu.univ.erp.domain.Role;
import edu.univ.erp.domain.User;
import edu.univ.erp.service.AdminService;
import edu.univ.erp.service.InstructorService;
import edu.univ.erp.service.SettingsService;
import edu.univ.erp.service.StudentService;
import edu.univ.erp.ui.kit.DropdownMenu;
import edu.univ.erp.ui.kit.Modal;
import edu.univ.erp.ui.kit.Stage;
import edu.univ.erp.ui.kit.Toasts;
import edu.univ.erp.ui.screen.AppFrame;
import edu.univ.erp.ui.theme.Prefs;
import edu.univ.erp.ui.theme.Theme;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/** The signed-in workspace: sidebar, top bar and the animated page stage. */
public class Shell extends JPanel {

    private final AppFrame frame;
    private final User user;
    private final List<Nav> navs;
    private final Map<String, Page> pages = new HashMap<>();
    private final Sidebar sidebar;
    private final TopBar top;
    private final Stage stage = new Stage();
    private String currentId;
    private final Timer poll;
    private boolean maintenance;
    private List<Announcement> notices = List.of();

    public Shell(AppFrame frame, User user) {
        super(new BorderLayout());
        this.frame = frame;
        this.user = user;
        this.navs = Navs.forRole(user.role());
        setOpaque(false);
        sidebar = new Sidebar(navs, user, n -> go(n.id()), this::userMenu);
        top = new TopBar(user.displayName());
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(top, BorderLayout.NORTH);
        main.add(stage, BorderLayout.CENTER);
        add(sidebar, BorderLayout.WEST);
        add(main, BorderLayout.CENTER);

        top.search.addActionListener(e -> palette());
        top.bell.addActionListener(e -> noticesMenu());
        top.theme.addActionListener(e -> toggleTheme());
        top.avatar.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                userMenu(top.avatar);
            }
        });
        keys();
        Theme.onChange(themeIcon);
        go(navs.get(0).id());
        poll = new Timer(30_000, e -> pollStatus());
        poll.setInitialDelay(300);
        poll.start();
    }

    public User user() {
        return user;
    }

    public Stage stage() {
        return stage;
    }

    public AppFrame frame() {
        return frame;
    }

    public boolean maintenance() {
        return maintenance;
    }

    /** Navigates to a page by id, sliding in the direction of travel. */
    public void go(String id) {
        int to = indexOf(id);
        if (to < 0) return;
        int from = currentId == null ? -1 : indexOf(currentId);
        Nav nav = navs.get(to);
        Page page = pages.computeIfAbsent(id, k -> nav.factory().apply(this));
        currentId = id;
        sidebar.select(id);
        top.setTitle(page.title(), page.subtitle());
        page.onShow();
        stage.show(page, from < 0 ? 0 : to >= from ? 1 : -1);
        if (id.equals("notices")) markNoticesSeen();
    }

    public Page page(String id) {
        return pages.get(id);
    }

    public void refreshTitle(Page p) {
        if (pages.get(currentId) == p) top.setTitle(p.title(), p.subtitle());
    }

    private int indexOf(String id) {
        for (int i = 0; i < navs.size(); i++) if (navs.get(i).id().equals(id)) return i;
        return -1;
    }

    private void keys() {
        bind("control K", this::palette);
        bind("control B", sidebar::toggleCollapsed);
        bind("control shift D", this::toggleTheme);
        for (int i = 0; i < Math.min(9, navs.size()); i++) {
            String id = navs.get(i).id();
            bind("control " + (i + 1), () -> go(id));
        }
    }

    private void bind(String key, Runnable r) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
        getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                r.run();
            }
        });
    }

    private final Runnable themeIcon = this::updateThemeIcon;

    private void updateThemeIcon() {
        top.theme.setIconName(Theme.isDark() ? "sun" : "moon");
        // Pages not currently on screen are detached from the window, so update them too
        for (Page p : pages.values()) {
            if (p != stage.current()) javax.swing.SwingUtilities.updateComponentTreeUI(p);
        }
    }

    private void toggleTheme() {
        Theme.toggle(frame);
    }

    private void palette() {
        List<CommandPalette.Command> cmds = new ArrayList<>();
        for (int i = 0; i < navs.size(); i++) {
            Nav n = navs.get(i);
            cmds.add(new CommandPalette.Command(n.icon(), n.label(), i < 9 ? "Ctrl " + (i + 1) : n.group(), () -> go(n.id())));
        }
        cmds.add(new CommandPalette.Command(Theme.isDark() ? "sun" : "moon", "Switch light / dark theme",
                "Ctrl Shift D", this::toggleTheme));
        cmds.add(new CommandPalette.Command("panel", "Collapse / expand sidebar", "Ctrl B", sidebar::toggleCollapsed));
        for (Prefs.Transition t : Prefs.Transition.values()) {
            String label = switch (t) {
                case SLIDE -> "Page transition: Slide";
                case FADE -> "Page transition: Fade & zoom";
                case CUBE -> "Page transition: 3D cube";
            };
            cmds.add(new CommandPalette.Command("cube", label, Prefs.transition() == t ? "current" : "preference", () -> {
                Prefs.transition(t);
                Toasts.info(this, "Page transitions set to " + label.substring(label.indexOf(':') + 2) + ".");
            }));
        }
        cmds.add(new CommandPalette.Command("logout", "Sign out", "account", this::signOut));
        CommandPalette.open(this, cmds);
    }

    private void userMenu(JComponent anchor) {
        DropdownMenu m = new DropdownMenu().width(250);
        m.add(DropdownMenu.Item.rich("user", user.displayName(), user.role().label() + " · @" + user.username(), null));
        m.add(DropdownMenu.Item.separator());
        m.add(DropdownMenu.Item.of("user", "My profile", () -> go("profile")));
        m.add(DropdownMenu.Item.of("sliders", "Preferences", () -> go("profile")));
        m.add(DropdownMenu.Item.of(Theme.isDark() ? "sun" : "moon", Theme.isDark() ? "Light theme" : "Dark theme",
                this::toggleTheme));
        m.add(DropdownMenu.Item.of("command", "Quick jump  (Ctrl K)", this::palette));
        m.add(DropdownMenu.Item.separator());
        m.add(DropdownMenu.Item.danger("logout", "Sign out", this::signOut));
        m.show(anchor, anchor == top.avatar ? DropdownMenu.Align.RIGHT : DropdownMenu.Align.LEFT);
    }

    public void signOut() {
        Modal.confirm(this, "Sign out?", "You'll return to the sign-in screen.", "Sign out", false, () -> {
            poll.stop();
            frame.signOut();
        });
    }

    // ---- notices and status -------------------------------------------------------------

    public List<Announcement> loadNotices() {
        return switch (user.role()) {
            case STUDENT -> StudentService.announcements(30);
            case INSTRUCTOR -> InstructorService.announcements(30);
            case ADMIN -> AdminService.announcements(30);
        };
    }

    private Preferences seenPrefs() {
        return Preferences.userRoot().node("campus-erp/seen");
    }

    private int unread() {
        int seen = seenPrefs().getInt("u" + user.id(), 0);
        return (int) notices.stream().filter(a -> a.id() > seen).count();
    }

    public void markNoticesSeen() {
        int max = notices.stream().mapToInt(Announcement::id).max().orElse(0);
        seenPrefs().putInt("u" + user.id(), Math.max(max, seenPrefs().getInt("u" + user.id(), 0)));
        top.bell.setCount(0);
        sidebar.setBadge("notices", 0);
    }

    public void pollStatus() {
        new SwingWorker<Object[], Void>() {
            @Override
            protected Object[] doInBackground() {
                boolean m = SettingsService.get().maintenance();
                return new Object[]{m, loadNotices()};
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void done() {
                try {
                    Object[] r = get();
                    boolean m = (Boolean) r[0];
                    if (m != maintenance && user.role() != Role.ADMIN) {
                        if (m) Toasts.warning(Shell.this, "The system is now in maintenance mode. You can look around, but changes are paused.");
                        else Toasts.success(Shell.this, "Maintenance is over. Everything is back to normal.");
                    }
                    maintenance = m;
                    top.maintenance.setVisible(m);
                    top.revalidate();
                    notices = (List<Announcement>) r[1];
                    int u = "notices".equals(currentId) ? 0 : unread();
                    if ("notices".equals(currentId)) markNoticesSeen();
                    top.bell.setCount(u);
                    sidebar.setBadge("notices", u);
                } catch (Exception ignored) {
                    // offline: keep the last known state
                }
            }
        }.execute();
    }

    private void noticesMenu() {
        DropdownMenu m = new DropdownMenu().width(360).maxHeight(420);
        m.add(DropdownMenu.Item.header("Latest notices"));
        int shown = 0;
        for (Announcement a : notices) {
            if (shown++ == 6) break;
            String where = a.isCampusWide() ? "Campus" : a.sectionLabel();
            m.add(DropdownMenu.Item.rich(a.isCampusWide() ? "megaphone" : "book", a.title(),
                    a.authorName() + " · " + where + " · " + ago(a.createdAt()), () -> go("notices")));
        }
        if (notices.isEmpty()) m.add(DropdownMenu.Item.header("You're all caught up"));
        m.add(DropdownMenu.Item.separator());
        m.add(DropdownMenu.Item.of("arrow-right", "View all notices", () -> go("notices")));
        m.show(top.bell, DropdownMenu.Align.RIGHT);
        markNoticesSeen();
    }

    public static String ago(LocalDateTime t) {
        if (t == null) return "";
        Duration d = Duration.between(t, LocalDateTime.now());
        if (d.toMinutes() < 1) return "just now";
        if (d.toMinutes() < 60) return d.toMinutes() + "m ago";
        if (d.toHours() < 24) return d.toHours() + "h ago";
        if (d.toDays() < 30) return d.toDays() + "d ago";
        return t.toLocalDate().toString();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        Theme.Palette p = Theme.p();
        g2.setColor(p.bg());
        g2.fillRect(0, 0, getWidth(), getHeight());
        float r = Math.max(500, getWidth() * 0.6f);
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(getWidth(), 0), r, new float[]{0f, 1f},
                new Color[]{Theme.alpha(p.accent(), Theme.isDark() ? 0.12 : 0.07), Theme.alpha(p.accent(), 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(Sidebar.EXPANDED, getHeight()), r * 0.8f,
                new float[]{0f, 1f}, new Color[]{Theme.alpha(p.accent2(), Theme.isDark() ? 0.07 : 0.05),
                Theme.alpha(p.accent2(), 0)}, MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    @Override
    public void removeNotify() {
        poll.stop();
        Theme.removeListener(themeIcon);
        super.removeNotify();
    }
}
