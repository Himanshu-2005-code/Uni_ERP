package edu.univ.erp.ui.chart;

import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;
import edu.univ.erp.ui.theme.Theme;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/** Shared chart behaviour: play-in animation when first shown or when data changes, hover and tooltips. */
public abstract class ChartBase extends JComponent {

    protected double progress;
    protected Point mouse;
    private Animator.Anim anim;
    private boolean played;
    private long duration = 1000;

    protected ChartBase() {
        setOpaque(false);
        setFont(Theme.font(11.5f));
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing() && !played) play();
        });
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouse = e.getPoint();
                onHover(mouse);
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mouse = null;
                onHover(null);
                repaint();
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    protected void onHover(Point p) {
    }

    @Override
    public java.awt.Dimension getMinimumSize() {
        return new java.awt.Dimension(80, 60);
    }

    protected void duration(long ms) {
        duration = ms;
    }

    /** Replays the entrance animation (call after changing data). */
    public void play() {
        played = true;
        if (anim != null) anim.cancel();
        progress = 0;
        anim = Animator.animate(duration, Easing.OUT_QUART, t -> {
            progress = t;
            repaint();
        }, null);
    }

    protected void dataChanged() {
        if (isShowing()) play();
        else {
            played = false;
            repaint();
        }
    }

    /** "Nice" axis step for roughly n ticks up to max. */
    public static double niceStep(double max, int n) {
        if (max <= 0) return 1;
        double raw = max / n;
        double mag = Math.pow(10, Math.floor(Math.log10(raw)));
        double norm = raw / mag;
        double nice = norm <= 1 ? 1 : norm <= 2 ? 2 : norm <= 2.5 ? 2.5 : norm <= 5 ? 5 : 10;
        return nice * mag;
    }

    public static String compact(double v) {
        if (Math.abs(v) >= 1_000_000) return String.format("%.1fM", v / 1_000_000);
        if (Math.abs(v) >= 10_000) return String.format("%.0fk", v / 1000);
        if (Math.abs(v) >= 1000) return String.format("%.1fk", v / 1000);
        if (v == Math.rint(v)) return String.valueOf((long) v);
        return String.format("%.1f", v);
    }

    /** Draws a floating tooltip box near (x, y) with a title and lines of "label: value". */
    protected void tooltip(Graphics2D g2, int x, int y, String title, List<String[]> lines, List<Color> dots) {
        Theme.Palette p = Theme.p();
        g2.setFont(Theme.semibold(12f));
        FontMetrics fb = g2.getFontMetrics();
        g2.setFont(Theme.font(12f));
        FontMetrics fr = g2.getFontMetrics();
        int w = fb.stringWidth(title) + 24;
        for (String[] l : lines) w = Math.max(w, fr.stringWidth(l[0]) + fb.stringWidth(l[1]) + 52);
        int h = 30 + lines.size() * 20;
        int bx = x + 14;
        int by = y - h - 10;
        if (bx + w > getWidth() - 4) bx = x - w - 14;
        if (by < 4) by = y + 14;
        bx = Math.max(4, bx);
        Color bg = Theme.isDark() ? Theme.hex("#1C2540") : Theme.hex("#0F172A");
        for (int i = 4; i >= 1; i--) {
            g2.setColor(new Color(0, 0, 0, 10));
            g2.fill(new RoundRectangle2D.Double(bx - i, by - i + 2, w + 2 * i, h + 2 * i, 14 + i, 14 + i));
        }
        g2.setColor(bg);
        g2.fill(new RoundRectangle2D.Double(bx, by, w, h, 12, 12));
        g2.setFont(Theme.semibold(12f));
        g2.setColor(new Color(0xF8FAFC));
        g2.drawString(title, bx + 12, by + 20);
        int ly = by + 40;
        for (int i = 0; i < lines.size(); i++) {
            String[] l = lines.get(i);
            int lx = bx + 12;
            if (dots != null && i < dots.size() && dots.get(i) != null) {
                g2.setColor(dots.get(i));
                g2.fillOval(lx, ly - 9, 9, 9);
                lx += 16;
            }
            g2.setFont(Theme.font(12f));
            g2.setColor(new Color(0xCBD5E1));
            g2.drawString(l[0], lx, ly);
            g2.setFont(Theme.semibold(12f));
            g2.setColor(Color.WHITE);
            g2.drawString(l[1], bx + w - 12 - g2.getFontMetrics().stringWidth(l[1]), ly);
            ly += 20;
        }
        p.hashCode();
    }

    protected static String ellipsize(String s, FontMetrics fm, int w) {
        if (fm.stringWidth(s) <= w) return s;
        String t = s;
        while (t.length() > 1 && fm.stringWidth(t + "…") > w) t = t.substring(0, t.length() - 1);
        return t + "…";
    }
}
