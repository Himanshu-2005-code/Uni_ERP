package edu.univ.erp.ui.page.common;

import edu.univ.erp.domain.InstructorProfile;
import edu.univ.erp.domain.Role;
import edu.univ.erp.domain.StudentProfile;
import edu.univ.erp.domain.User;
import edu.univ.erp.service.AuthService;
import edu.univ.erp.service.InstructorService;
import edu.univ.erp.service.Session;
import edu.univ.erp.service.StudentService;
import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;
import edu.univ.erp.ui.kit.Async;
import edu.univ.erp.ui.kit.Avatar;
import edu.univ.erp.ui.kit.Badge;
import edu.univ.erp.ui.kit.Btn;
import edu.univ.erp.ui.kit.Card;
import edu.univ.erp.ui.kit.Segmented;
import edu.univ.erp.ui.kit.Toasts;
import edu.univ.erp.ui.kit.Toggle;
import edu.univ.erp.ui.kit.UI;
import edu.univ.erp.ui.page.Page;
import edu.univ.erp.ui.page.Parts;
import edu.univ.erp.ui.page.Shell;
import edu.univ.erp.ui.theme.Prefs;
import edu.univ.erp.ui.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** The signed-in user's details, password and app preferences. */
public class ProfilePage extends Page {

    private record Data(User user, StudentProfile student, InstructorProfile instructor) {
    }

    public ProfilePage(Shell shell) {
        super(shell);
    }

    @Override
    public String title() {
        return "Profile & settings";
    }

    @Override
    public String subtitle() {
        return "Your account and how the app looks and feels";
    }

    @Override
    public void reload() {
        load(() -> new Data(Session.user(),
                Session.user().role() == Role.STUDENT ? StudentService.profile() : null,
                Session.user().role() == Role.INSTRUCTOR ? InstructorService.profile() : null), this::render);
    }

    private void render(Data d) {
        JPanel col = column();
        JPanel grid = new JPanel(new MigLayout("insets 0, fillx, gap 18", "[340!, fill][grow, fill]", "[top]"));
        grid.setOpaque(false);

        // Identity card
        User u = d.user();
        Card id = new Card(new MigLayout("insets 0, fillx, wrap 1, alignx center", "[grow, center]", "[]14[]4[]10[]18[]"));
        id.add(new JLabel(new Avatar(u.displayName(), 92)));
        id.add(UI.h2(u.displayName()));
        id.add(UI.muted("@" + u.username() + (u.email() == null ? "" : "  ·  " + u.email())));
        id.add(new Badge(u.role().label(), Parts.statusColor(u.role().name())).dot());
        String[][] facts;
        if (d.student() != null) {
            facts = new String[][]{{"Roll number", d.student().rollNo()}, {"Program", nz(d.student().program())},
                    {"Batch", String.valueOf(d.student().year())}, {"Last sign-in", Parts.dateTime(u.lastLogin())}};
        } else if (d.instructor() != null) {
            facts = new String[][]{{"Department", nz(d.instructor().department())}, {"Office", nz(d.instructor().office())},
                    {"Last sign-in", Parts.dateTime(u.lastLogin())}};
        } else {
            facts = new String[][]{{"Member since", Parts.dateTime(u.createdAt())}, {"Last sign-in", Parts.dateTime(u.lastLogin())}};
        }
        JPanel f = Parts.facts(facts);
        id.add(f, "growx");
        JPanel left = new JPanel(new MigLayout("insets 0, fillx, wrap 1, gap 18", "[grow, fill]", ""));
        left.setOpaque(false);
        left.add(id);
        left.add(Parts.section("Keyboard shortcuts", null, Parts.facts(new String[][]{{"Quick jump", "Ctrl K"},
                {"Go to page", "Ctrl 1–9"}, {"Toggle sidebar", "Ctrl B"}, {"Light / dark", "Ctrl Shift D"},
                {"Close dialog", "Esc"}})));
        grid.add(left);

        JPanel right = new JPanel(new MigLayout("insets 0, fillx, wrap 1, gap 18", "[grow, fill]", ""));
        right.setOpaque(false);
        right.add(contact(u));
        right.add(password());
        right.add(preferences());
        grid.add(right);
        col.add(grid);
        setBody(col, true);
    }

    private JComponent contact(User u) {
        JPanel form = new JPanel(new MigLayout("insets 0, fillx, gap 14", "[grow, fill][grow, fill]", ""));
        form.setOpaque(false);
        JTextField name = UI.field("Full name", "user");
        name.setText(u.fullName() == null ? "" : u.fullName());
        JTextField email = UI.field("name@example.edu", "mail");
        email.setText(u.email() == null ? "" : u.email());
        form.add(UI.labeled("Full name", name, null));
        form.add(UI.labeled("Email", email, null));
        Btn save = Btn.primary("Save details", "check");
        save.addActionListener(e -> Async.button(save, () -> AuthService.updateContact(name.getText(), email.getText()),
                fresh -> {
                    Toasts.success(this, "Your details are saved.");
                    reload();
                }));
        return Parts.section("Contact details", "Shown to instructors and administrators", form, save);
    }

    private JComponent password() {
        JPanel form = new JPanel(new MigLayout("insets 0, fillx, wrap 3, gap 14", "[grow, fill][grow, fill][grow, fill]", ""));
        form.setOpaque(false);
        JPasswordField cur = UI.password("Current password");
        JPasswordField next = UI.password("New password");
        JPasswordField again = UI.password("Repeat new password");
        form.add(UI.labeled("Current", cur, null));
        form.add(UI.labeled("New", next, null));
        form.add(UI.labeled("Confirm", again, null));
        Strength meter = new Strength();
        form.add(meter, "span 3, h 30!");
        next.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                meter.set(new String(next.getPassword()));
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                meter.set(new String(next.getPassword()));
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        Btn save = Btn.primary("Change password", "key");
        save.addActionListener(e -> {
            String n = new String(next.getPassword());
            if (!n.equals(new String(again.getPassword()))) {
                Toasts.error(this, "The new passwords don't match.");
                return;
            }
            Async.button(save, () -> {
                AuthService.changePassword(new String(cur.getPassword()), n);
                return true;
            }, ok -> {
                cur.setText("");
                next.setText("");
                again.setText("");
                meter.set("");
                Toasts.success(this, "Password changed. Use it next time you sign in.");
            });
        });
        return Parts.section("Password", "At least 8 characters with letters and numbers", form, save);
    }

    private JComponent preferences() {
        JPanel p = new JPanel(new MigLayout("insets 0, fillx, wrap 2, gapy 18", "[grow][]", "[center]"));
        p.setOpaque(false);
        Toggle dark = new Toggle(Theme.isDark());
        dark.onChange(on -> {
            if (on != Theme.isDark()) Theme.toggle(shell.frame());
        });
        row(p, "Dark theme", "Easy on the eyes at night", dark);

        Segmented tr = new Segmented("Slide", "Fade & zoom", "3D cube");
        tr.select(Prefs.transition().ordinal(), false);
        tr.onChange(i -> {
            Prefs.transition(Prefs.Transition.values()[i]);
            previewTransition();
        });
        row(p, "Page transitions", "How pages move when you navigate (changing it plays a preview)", tr);

        Toggle motion = new Toggle(Prefs.reduceMotion());
        motion.onChange(Prefs::reduceMotion);
        row(p, "Reduce motion", "Turns off animations and transitions", motion);

        Toggle collapsed = new Toggle(Prefs.sidebarCollapsed());
        collapsed.onChange(on -> {
            if (on != Prefs.sidebarCollapsed()) toggleSidebar();
        });
        row(p, "Compact sidebar", "Icons only, more room for content", collapsed);
        return Parts.section("Appearance", "Saved on this computer", p);
    }

    private void toggleSidebar() {
        javax.swing.Action a = shell.getActionMap().get("control B");
        if (a != null) a.actionPerformed(null);
    }

    private void previewTransition() {
        String back = "profile";
        shell.go("dashboard");
        javax.swing.Timer t = new javax.swing.Timer(900, e -> shell.go(back));
        t.setRepeats(false);
        t.start();
    }

    private static void row(JPanel p, String title, String sub, JComponent control) {
        JPanel t = new JPanel(new MigLayout("insets 0, wrap 1, gap 2", "", ""));
        t.setOpaque(false);
        t.add(UI.h3(title));
        t.add(UI.muted(sub));
        p.add(t, "growx");
        p.add(control);
    }

    private static String nz(String s) {
        return s == null ? "—" : s;
    }

    /** Animated password strength bar. */
    private static final class Strength extends JComponent {
        private double level;
        private double shown;
        private String label = "";
        private Animator.Anim anim;

        void set(String pw) {
            int score = 0;
            if (pw.length() >= 8) score++;
            if (pw.length() >= 12) score++;
            if (pw.matches(".*[a-z].*") && pw.matches(".*[A-Z].*")) score++;
            if (pw.matches(".*\\d.*")) score++;
            if (pw.matches(".*[^A-Za-z0-9].*")) score++;
            level = pw.isEmpty() ? 0 : Math.max(0.12, score / 5.0);
            label = pw.isEmpty() ? "" : score <= 1 ? "Weak" : score == 2 ? "Fair" : score == 3 ? "Good" : "Strong";
            if (anim != null) anim.cancel();
            double from = shown;
            anim = Animator.animate(400, Easing.OUT_CUBIC, t -> {
                shown = Animator.lerp(from, level, t);
                repaint();
            }, null);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.hq(g);
            Theme.Palette p = Theme.p();
            int w = getWidth() - 80;
            g2.setColor(Theme.isDark() ? p.border() : Theme.hex("#E8EBF3"));
            g2.fill(new RoundRectangle2D.Double(0, 11, w, 6, 6, 6));
            Color c = shown < 0.35 ? p.danger() : shown < 0.55 ? p.warning() : shown < 0.75 ? Theme.series(0) : p.success();
            g2.setColor(c);
            g2.fill(new RoundRectangle2D.Double(0, 11, w * shown, 6, 6, 6));
            g2.setFont(Theme.semibold(12f));
            g2.drawString(label, w + 14, 19);
            g2.dispose();
        }
    }
}
