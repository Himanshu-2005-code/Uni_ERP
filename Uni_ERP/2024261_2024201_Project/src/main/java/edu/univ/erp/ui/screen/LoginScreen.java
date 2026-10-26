package edu.univ.erp.ui.screen;

import edu.univ.erp.config.AppConfig;
import edu.univ.erp.db.DemoSeeder;
import edu.univ.erp.domain.User;
import edu.univ.erp.service.AuthService;
import edu.univ.erp.service.SettingsService;
import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;
import edu.univ.erp.ui.kit.Brand;
import edu.univ.erp.ui.kit.Btn;
import edu.univ.erp.ui.kit.Icons;
import edu.univ.erp.ui.kit.Toasts;
import edu.univ.erp.ui.kit.Toggle;
import edu.univ.erp.ui.theme.Prefs;
import edu.univ.erp.ui.theme.Theme;
import edu.univ.erp.ui.three.LoginScene;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Sign-in over the animated 3D scene. */
class LoginScreen extends JLayeredPane {

    private final AppFrame frame;
    private final LoginScene scene = new LoginScene();
    private final JTextField user = Glass.field("Username", "user");
    private final JPasswordField pass = Glass.password("Password");
    private final Toggle remember = new Toggle(!Prefs.lastUsername().isEmpty());
    private final Btn submit = Btn.primary("Sign in", "arrow-right");
    private final JLabel error = Glass.label(" ", 12.5f, false, Theme.hex("#FCA5A5"));
    private final Glass.Card card;
    private final List<Reveal> reveals = new ArrayList<>();
    private AWTEventListener pointer;

    LoginScreen(AppFrame frame, String notice) {
        this.frame = frame;
        try {
            SettingsService.get().refresh();
        } catch (RuntimeException ignored) {
            // shown with defaults; the database is checked again on sign-in
        }
        add(scene, Integer.valueOf(0));

        JPanel overlay = new JPanel(new MigLayout("insets 0 64 0 64, fill", "[grow 60, fill][460!]", "[grow, center]"));
        overlay.setOpaque(false);
        overlay.add(brandColumn(), "aligny center, gapright 40");
        card = buildCard();
        overlay.add(card, "aligny center, h pref!");
        add(overlay, Integer.valueOf(1));

        user.setText(Prefs.lastUsername());
        submit.addActionListener(e -> attempt(user.getText(), new String(pass.getPassword())));
        pass.addActionListener(e -> submit.doClick());
        user.addActionListener(e -> pass.requestFocusInWindow());

        card.alpha = 0;
        card.offsetY = 30;
        Animator.animate(700, Easing.OUT_BACK, t -> {
            card.alpha = (float) Math.min(1, t * 1.5);
            card.offsetY = 30 * (1 - t);
            card.repaint();
        }, null);
        for (int i = 0; i < reveals.size(); i++) reveals.get(i).play(120 + i * 110L);
        SwingUtilities.invokeLater(() -> (user.getText().isEmpty() ? user : pass).requestFocusInWindow());
        if (notice != null) SwingUtilities.invokeLater(() -> Toasts.success(this, notice));
    }

    @Override
    public void doLayout() {
        for (Component c : getComponents()) c.setBounds(0, 0, getWidth(), getHeight());
    }

    @Override
    public void addNotify() {
        super.addNotify();
        pointer = ev -> {
            if (ev instanceof MouseEvent me && me.getComponent() != null && isShowing()
                    && SwingUtilities.isDescendingFrom(me.getComponent(), this)) {
                Point p = SwingUtilities.convertPoint(me.getComponent(), me.getPoint(), this);
                scene.pointer(p.x / (double) Math.max(1, getWidth()), p.y / (double) Math.max(1, getHeight()));
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(pointer, AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    @Override
    public void removeNotify() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(pointer);
        super.removeNotify();
    }

    private JComponent brandColumn() {
        JPanel col = new JPanel(new MigLayout("insets 0, wrap 1, gapy 0", "[grow, fill]", ""));
        col.setOpaque(false);
        Reveal pill = new Reveal(new Pill("University ERP suite  ·  v2.0"));
        col.add(pill, "w pref!, gapbottom 22");
        Reveal head = new Reveal(new Headline());
        col.add(head, "h 128!, gapbottom 18");
        String inst = SettingsService.get().institution();
        Reveal sub = new Reveal(Glass.label("<html><div style='width:331px;line-height:1.5'>Registration, grading, timetables and "
                + "analytics for " + inst + " — fast, secure and genuinely pleasant to use.</div></html>", 15f, false, Glass.MUTED));
        col.add(sub, "gapbottom 34");
        String[][] feats = {{"shield", "Clash, credit and seat checks on every registration"},
                {"bars", "Live gradebook with instant analytics"},
                {"cube", "Interactive 3D insights you can spin and explore"}};
        for (String[] f : feats) {
            JPanel row = new JPanel(new MigLayout("insets 0, gap 14", "[][grow]", "[center]"));
            row.setOpaque(false);
            row.add(new JLabel(new GlassIcon(f[0])));
            row.add(Glass.label(f[1], 14f, false, Glass.TEXT));
            Reveal r = new Reveal(row);
            col.add(r, "gapbottom 14");
        }
        return col;
    }

    private Glass.Card buildCard() {
        Glass.Card c = new Glass.Card(new MigLayout("insets 38 38 32 38, fillx, wrap 1", "[grow, fill]", ""));
        JLabel mark = new JLabel(new javax.swing.Icon() {
            @Override
            public void paintIcon(Component cc, Graphics g, int x, int y) {
                Graphics2D g2 = Theme.hq(g);
                Brand.paintMark(g2, x, y, 46);
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 46;
            }

            @Override
            public int getIconHeight() {
                return 46;
            }
        });
        c.add(mark, "gapbottom 18");
        c.add(Glass.label("Welcome back", 26f, true, Glass.TEXT));
        c.add(Glass.label("Sign in to " + SettingsService.get().institution(), 13.5f, false, Glass.MUTED), "gapbottom 24");
        c.add(Glass.label("Username", 12.5f, true, Glass.MUTED), "gapbottom 6");
        c.add(user, "h 46!, gapbottom 14");
        c.add(Glass.label("Password", 12.5f, true, Glass.MUTED), "gapbottom 6");
        c.add(pass, "h 46!, gapbottom 6");
        c.add(error, "gapbottom 8");
        JPanel row = new JPanel(new MigLayout("insets 0, fillx", "[]10[]push[]", "[center]"));
        row.setOpaque(false);
        row.add(remember);
        row.add(Glass.label("Remember me", 13f, false, Glass.MUTED));
        JLabel forgot = Glass.label("Forgot password?", 13f, true, Theme.hex("#A5B4FC"));
        forgot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgot.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Toasts.info(LoginScreen.this, "Ask your administrator to reset it from Users → Reset password.");
            }
        });
        row.add(forgot);
        c.add(row, "gapbottom 22");
        c.add(submit, "h 48!");

        if (SettingsService.get().demoData()) {
            c.add(Glass.label("Try a demo account", 12f, true, Glass.MUTED), "gaptop 22, gapbottom 8");
            JPanel chips = new JPanel(new MigLayout("insets 0, fillx, gap 8", "[grow, fill][grow, fill][grow, fill]", ""));
            chips.setOpaque(false);
            chips.add(chip("cap", "Student", "stu1"));
            chips.add(chip("book", "Instructor", "inst1"));
            chips.add(chip("shield", "Admin", "admin1"));
            c.add(chips);
        }
        AppConfig cfg = AppConfig.get();
        JLabel conn = Glass.label("● Connected to " + cfg.getHost() + ":" + cfg.getPort() + "   ·   Database settings",
                11.5f, false, Theme.alpha(Glass.MUTED, 0.85));
        conn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        conn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                frame.showConnect(null);
            }
        });
        c.add(conn, "gaptop 22, center");
        return c;
    }

    private JComponent chip(String icon, String label, String username) {
        Btn b = new Btn(label, icon, Btn.Kind.GHOST) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Theme.hq(g);
                boolean hot = getModel().isRollover();
                g2.setColor(new Color(255, 255, 255, hot ? 30 : 14));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
                g2.setColor(new Color(255, 255, 255, hot ? 60 : 30));
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(getText()) + 22;
                int x = (getWidth() - tw) / 2;
                Icons.paint(g2, icon, x, (getHeight() - 15) / 2.0, 15, Theme.hex("#A5B4FC"), 1.8f);
                g2.setColor(Glass.TEXT);
                g2.drawString(getText(), x + 22, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        b.setFont(Theme.medium(12.5f));
        b.setToolTipText("Sign in as " + username + " (password " + DemoSeeder.DEMO_PASSWORD + ")");
        b.addActionListener(e -> {
            user.setText(username);
            pass.setText(DemoSeeder.DEMO_PASSWORD);
            attempt(username, DemoSeeder.DEMO_PASSWORD);
        });
        b.setPreferredSize(new Dimension(10, 40));
        return b;
    }

    private void attempt(String u, String p) {
        if (submit.isLoading()) return;
        error.setText(" ");
        submit.setLoading(true);
        new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() {
                return AuthService.login(u, p);
            }

            @Override
            protected void done() {
                submit.setLoading(false);
                try {
                    User signed = get();
                    Prefs.lastUsername(remember.isOn() ? signed.username() : "");
                    submit.setIconName("check");
                    frame.showShell(signed);
                } catch (Exception e) {
                    Throwable c = e.getCause() == null ? e : e.getCause();
                    error.setText(c.getMessage() == null ? "Couldn't sign in." : c.getMessage());
                    pass.setText("");
                    pass.requestFocusInWindow();
                    shake();
                }
            }
        }.execute();
    }

    private void shake() {
        Animator.animate(520, Easing.LINEAR, t -> {
            card.offsetX = Math.sin(t * Math.PI * 7) * 12 * (1 - t);
            card.repaint();
        }, () -> {
            card.offsetX = 0;
            card.repaint();
        });
    }

    /** Fades and lifts a child into place after a delay. */
    private final class Reveal extends JPanel {
        private float alpha;
        private double dy = 18;

        Reveal(JComponent child) {
            super(new java.awt.BorderLayout());
            setOpaque(false);
            add(child);
            reveals.add(this);
        }

        void play(long delay) {
            javax.swing.Timer t = new javax.swing.Timer((int) delay, e -> Animator.animate(650, Easing.OUT_CUBIC, v -> {
                alpha = (float) v;
                dy = 18 * (1 - v);
                repaint();
            }, null));
            t.setRepeats(false);
            t.start();
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0, Math.min(1, alpha))));
            g2.translate(0, dy);
            super.paint(g2);
            g2.dispose();
        }
    }

    /** Two-line hero headline with a gradient on the second line. */
    private static final class Headline extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.hq(g);
            g2.setFont(Theme.bold(48f));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString("Your whole campus,", 0, fm.getAscent());
            String l2 = "beautifully in sync.";
            int w = fm.stringWidth(l2);
            g2.setPaint(new GradientPaint(0, 0, Theme.hex("#A5B4FC"), w, 0, Theme.hex("#67E8F9")));
            g2.drawString(l2, 0, fm.getAscent() + fm.getHeight());
            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(520, 128);
        }
    }

    private static final class Pill extends JComponent {
        private final String text;

        Pill(String text) {
            this.text = text;
            setFont(Theme.semibold(12f));
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(getFontMetrics(getFont()).stringWidth(text) + 40, 30);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.hq(g);
            g2.setColor(new Color(255, 255, 255, 18));
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, 29, 30, 30));
            g2.setColor(new Color(255, 255, 255, 40));
            g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, 29, 30, 30));
            g2.setColor(Theme.hex("#34D399"));
            g2.fillOval(14, 11, 8, 8);
            g2.setFont(getFont());
            g2.setColor(Glass.TEXT);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, 30, (30 - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    private static final class GlassIcon implements javax.swing.Icon {
        private final String name;

        GlassIcon(String name) {
            this.name = name;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = Theme.hq(g);
            g2.setColor(new Color(255, 255, 255, 18));
            g2.fill(new RoundRectangle2D.Double(x, y, 40, 40, 14, 14));
            g2.setColor(new Color(255, 255, 255, 36));
            g2.draw(new RoundRectangle2D.Double(x, y, 39, 39, 14, 14));
            Icons.paint(g2, name, x + 10, y + 10, 20, Theme.hex("#A5B4FC"), 1.9f);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return 40;
        }

        @Override
        public int getIconHeight() {
            return 40;
        }
    }
}
