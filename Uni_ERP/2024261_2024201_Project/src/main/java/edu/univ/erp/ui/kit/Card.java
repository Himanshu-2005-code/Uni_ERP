package edu.univ.erp.ui.kit;

import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;
import edu.univ.erp.ui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/** A rounded surface with a soft layered shadow; optionally lifts on hover. */
public class Card extends JPanel {

    public static final int SHADOW = 10;

    private final int radius;
    private double lift;
    private boolean hoverLift;
    private Animator.Anim anim;
    private Color tint;

    public Card(LayoutManager layout) {
        this(layout, 18);
    }

    public Card(LayoutManager layout, int radius) {
        super(layout);
        this.radius = radius;
        setOpaque(false);
        pad(20);
    }

    public Card pad(int p) {
        return pad(p, p, p, p);
    }

    public Card pad(int top, int left, int bottom, int right) {
        setBorder(BorderFactory.createEmptyBorder(top + SHADOW / 2, left + SHADOW / 2, bottom + SHADOW, right + SHADOW / 2));
        return this;
    }

    /** Tints the background (e.g. a soft accent colour for highlighted cards). */
    public Card tint(Color c) {
        this.tint = c;
        return this;
    }

    public Card hoverLift() {
        if (hoverLift) return this;
        hoverLift = true;
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                go(1);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!contains(e.getPoint())) go(0);
            }
        };
        addMouseListener(m);
        return this;
    }

    /** Propagates hover from child components so the lift doesn't flicker. */
    public void trackChildHover(java.awt.Component c) {
        c.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                go(1);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                java.awt.Point p = javax.swing.SwingUtilities.convertPoint(c, e.getPoint(), Card.this);
                if (!Card.this.contains(p)) go(0);
            }
        });
    }

    private void go(double target) {
        if (anim != null) anim.cancel();
        double from = lift;
        anim = Animator.animate(220, Easing.OUT_CUBIC, t -> {
            lift = Animator.lerp(from, target, t);
            repaint();
        }, null);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        paintSurface(g2, SHADOW / 2.0, SHADOW / 2.0, getWidth() - SHADOW, getHeight() - SHADOW * 1.5,
                radius, lift, tint);
        g2.dispose();
    }

    /** Paints a card surface; shared with components that draw their own cards. */
    public static void paintSurface(Graphics2D g2, double x, double y, double w, double h, int radius, double lift,
                                    Color tint) {
        Theme.Palette p = Theme.p();
        Color shadow = p.shadow();
        double base = Theme.isDark() ? 0.22 : 0.055;
        int layers = 7;
        for (int i = layers; i >= 1; i--) {
            double spread = i * (1.2 + lift * 0.6);
            double a = base * (1 + lift * 0.9) * Math.pow((layers - i + 1) / (double) layers, 2) / 2.2;
            g2.setColor(Theme.alpha(shadow, a));
            g2.fill(new RoundRectangle2D.Double(x - spread / 2, y - spread / 2 + i * (0.9 + lift * 0.5), w + spread,
                    h + spread, radius + spread, radius + spread));
        }
        RoundRectangle2D r = new RoundRectangle2D.Double(x, y, w, h, radius * 2, radius * 2);
        g2.setColor(tint != null ? tint : p.surface());
        g2.fill(r);
        g2.setColor(Theme.mix(p.border(), p.accent(), lift * 0.35));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(r);
    }

}
