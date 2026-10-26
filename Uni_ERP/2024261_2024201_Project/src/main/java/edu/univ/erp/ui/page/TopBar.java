package edu.univ.erp.ui.page;

import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;
import edu.univ.erp.ui.kit.Avatar;
import edu.univ.erp.ui.kit.Badge;
import edu.univ.erp.ui.kit.Btn;
import edu.univ.erp.ui.kit.Icons;
import edu.univ.erp.ui.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/** Page title (cross-fades on navigation), command search, alerts, theme switch and profile. */
public class TopBar extends JPanel {

    final AnimatedTitle title = new AnimatedTitle();
    final Btn search = new SearchButton();
    final BellButton bell = new BellButton();
    final Btn theme = Btn.icon(Theme.isDark() ? "sun" : "moon", "Switch light/dark (Ctrl+Shift+D)");
    final AvatarButton avatar;
    final Badge maintenance = new Badge("Maintenance mode", Theme.p().warning()).dot();

    public TopBar(String name) {
        super(new MigLayout("insets 18 28 12 28, fillx", "[grow]push[]14[]6[]6[]10[]", "[center]"));
        setOpaque(false);
        avatar = new AvatarButton(name);
        add(title, "growx, h 52!");
        add(maintenance, "hidemode 3");
        add(search, "w 230!, h 40!");
        add(bell, "w 40!, h 40!");
        add(theme, "w 40!, h 40!");
        add(avatar, "w 40!, h 40!");
        maintenance.setVisible(false);
    }

    public void setTitle(String t, String sub) {
        title.set(t, sub);
    }

    /** Title and subtitle with a slide-and-fade between pages. */
    static final class AnimatedTitle extends JComponent {
        private String title = "";
        private String sub;
        private String oldTitle;
        private String oldSub;
        private double t = 1;
        private Animator.Anim anim;

        void set(String nt, String ns) {
            if (nt.equals(title) && java.util.Objects.equals(ns, sub)) return;
            oldTitle = title;
            oldSub = sub;
            title = nt;
            sub = ns;
            if (anim != null) anim.cancel();
            t = 0;
            anim = Animator.animate(420, Easing.OUT_CUBIC, v -> {
                t = v;
                repaint();
            }, null);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.hq(g);
            if (oldTitle != null && t < 1) draw(g2, oldTitle, oldSub, (float) (1 - t), -10 * t);
            draw(g2, title, sub, (float) t, 12 * (1 - t));
            g2.dispose();
        }

        private void draw(Graphics2D g, String tt, String ss, float alpha, double dy) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0, Math.min(1, alpha))));
            g2.translate(0, dy);
            Theme.Palette p = Theme.p();
            g2.setFont(Theme.semibold(22f));
            g2.setColor(p.text());
            FontMetrics fm = g2.getFontMetrics();
            int y = ss == null ? (getHeight() + fm.getAscent()) / 2 - 3 : 26;
            g2.drawString(tt, 0, y);
            if (ss != null) {
                g2.setFont(Theme.font(12.5f));
                g2.setColor(p.muted());
                g2.drawString(ss, 0, 46);
            }
            g2.dispose();
        }
    }

    /** Looks like a search field; opens the command palette. */
    static final class SearchButton extends Btn {
        SearchButton() {
            super("Search or jump to…", "search", Kind.SECONDARY);
            setFont(Theme.font(13f));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.hq(g);
            Theme.Palette p = Theme.p();
            int w = getWidth();
            int h = getHeight();
            g2.setColor(Theme.isDark() ? p.surfaceAlt() : p.surface());
            g2.fill(new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 14, 14));
            g2.setColor(getModel().isRollover() ? Theme.mix(p.border(), p.accent(), 0.4) : p.border());
            g2.draw(new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 14, 14));
            Icons.paint(g2, "search", 13, (h - 16) / 2.0, 16, p.muted(), 1.8f);
            g2.setFont(getFont());
            g2.setColor(p.subtle());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), 38, (h - fm.getHeight()) / 2 + fm.getAscent());
            String k = "Ctrl K";
            g2.setFont(Theme.semibold(10.5f));
            FontMetrics fk = g2.getFontMetrics();
            int kw = fk.stringWidth(k) + 12;
            g2.setColor(Theme.isDark() ? p.border() : Theme.hex("#EEF0F6"));
            g2.fill(new RoundRectangle2D.Double(w - kw - 8, (h - 22) / 2.0, kw, 22, 8, 8));
            g2.setColor(p.muted());
            g2.drawString(k, w - kw - 2, (h - fk.getHeight()) / 2 + fk.getAscent());
            g2.dispose();
        }
    }

    /** Bell icon with an unread count. */
    static final class BellButton extends Btn {
        private int count;
        private double pulse;

        BellButton() {
            super(null, "bell", Kind.GHOST);
            compact();
            setToolTipText("Notices");
        }

        void setCount(int c) {
            if (c > count) {
                Animator.animate(700, Easing.OUT_CUBIC, t -> {
                    pulse = t;
                    repaint();
                }, () -> pulse = 0);
            }
            count = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (count <= 0) return;
            Graphics2D g2 = Theme.hq(g);
            double cx = getWidth() - 12;
            double cy = 11;
            if (pulse > 0) {
                g2.setColor(Theme.alpha(Theme.hex("#F43F5E"), 0.5 * (1 - pulse)));
                double r = 6 + pulse * 10;
                g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
            }
            g2.setColor(Theme.hex("#F43F5E"));
            g2.fill(new Ellipse2D.Double(cx - 5, cy - 5, 10, 10));
            g2.setColor(Theme.p().bg());
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new Ellipse2D.Double(cx - 5, cy - 5, 10, 10));
            g2.dispose();
        }
    }

    /** Round avatar that opens the account menu. */
    static final class AvatarButton extends JComponent {
        private final String name;
        private boolean hover;

        AvatarButton(String name) {
            this.name = name;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(40, 40);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.hq(g);
            if (hover) {
                g2.setColor(Theme.alpha(Theme.p().accent(), 0.3));
                g2.fill(new Ellipse2D.Double(0, 0, 40, 40));
            }
            Avatar.paint(g2, name, 3, 3, 34);
            g2.setColor(new Color(255, 255, 255, 60));
            g2.draw(new Ellipse2D.Double(3, 3, 34, 34));
            g2.dispose();
        }
    }
}
