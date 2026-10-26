package edu.univ.erp.ui.kit;

import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;
import edu.univ.erp.ui.theme.Prefs;
import edu.univ.erp.ui.theme.Theme;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Hosts one page at a time and animates between pages. The outgoing and incoming
 * pages are captured as images and composited, so transitions stay smooth however
 * complex the pages are.
 */
public class Stage extends JPanel {

    private JComponent current;
    private BufferedImage from;
    private BufferedImage to;
    private double t = 1;
    private int dir = 1;
    private Prefs.Transition style = Prefs.Transition.SLIDE;
    private Animator.Anim anim;

    public Stage() {
        super(null);
        setOpaque(false);
    }

    public JComponent current() {
        return current;
    }

    @Override
    public void doLayout() {
        for (Component c : getComponents()) c.setBounds(0, 0, getWidth(), getHeight());
    }

    /** Shows a page. dir > 0 means "forward" (enters from the right), dir < 0 backwards. */
    public void show(JComponent next, int dir) {
        show(next, dir, Prefs.transition());
    }

    public void show(JComponent next, int dir, Prefs.Transition transition) {
        if (next == current) return;
        if (anim != null) {
            anim.cancel();
            finish();
        }
        JComponent prev = current;
        current = next;
        if (prev == null || getWidth() <= 0 || Prefs.reduceMotion()) {
            removeAll();
            add(next);
            next.setBounds(0, 0, getWidth(), getHeight());
            revalidate();
            repaint();
            return;
        }
        this.dir = dir == 0 ? 1 : dir;
        this.style = transition;
        from = snapshot(prev);
        removeAll();
        add(next);
        next.setBounds(0, 0, getWidth(), getHeight());
        next.validate();
        to = snapshot(next);
        next.setVisible(false);
        t = 0;
        long ms = style == Prefs.Transition.CUBE ? 720 : style == Prefs.Transition.FADE ? 360 : 480;
        Easing e = style == Prefs.Transition.CUBE ? Easing.IN_OUT_CUBIC : Easing.OUT_QUART;
        anim = Animator.animate(ms, e, v -> {
            t = v;
            repaint();
        }, this::finish);
    }

    /** Re-captures the incoming page mid-transition (e.g. after its data arrives). */
    public void refreshSnapshot() {
        if (anim != null && anim.isRunning() && current != null) {
            current.setVisible(true);
            to = snapshot(current);
            current.setVisible(false);
        }
    }

    private void finish() {
        from = null;
        to = null;
        t = 1;
        if (current != null) {
            current.setVisible(true);
            current.revalidate();
        }
        anim = null;
        repaint();
    }

    private BufferedImage snapshot(JComponent c) {
        int w = Math.max(1, getWidth());
        int h = Math.max(1, getHeight());
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        boolean vis = c.isVisible();
        c.setVisible(true);
        c.paint(g);
        c.setVisible(vis);
        g.dispose();
        return img;
    }

    @Override
    public void paint(Graphics g) {
        if (from == null || to == null) {
            super.paint(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        int w = getWidth();
        int h = getHeight();
        switch (style) {
            case FADE -> {
                draw(g2, from, 0, 0, 1 - t * 0.02, (float) (1 - t));
                draw(g2, to, 0, 0, 0.965 + 0.035 * t, (float) t);
            }
            case CUBE -> cube(g2, w, h);
            default -> {
                double shift = w * 0.22;
                draw(g2, from, -dir * shift * t, 0, 1, (float) Math.max(0, 1 - t * 1.6));
                draw(g2, to, dir * shift * (1 - t), 0, 1, (float) Math.min(1, t * 1.25));
            }
        }
        g2.dispose();
    }

    private void draw(Graphics2D g, BufferedImage img, double dx, double dy, double scale, float alpha) {
        if (alpha <= 0) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, alpha)));
        double cx = getWidth() / 2.0;
        double cy = getHeight() / 2.0;
        g2.translate(cx + dx, cy + dy);
        g2.scale(scale, scale);
        g2.translate(-cx, -cy);
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
    }

    /** Rotates the two pages as faces of a cube, drawing each face as perspective-scaled strips. */
    private void cube(Graphics2D g2, int w, int h) {
        double angle = Math.PI / 2 * t * dir;
        double half = w / 2.0;
        double camera = half + w * 1.6;
        double focal = camera - half;
        double cx = w / 2.0;
        double cy = h / 2.0;
        // A gentle pull-back in the middle of the turn sells the depth
        double zoom = 1 - 0.17 * Math.sin(Math.PI * t);
        face(g2, from, -angle, half, camera, focal, cx, cy, w, h, zoom);
        face(g2, to, Math.PI / 2 * dir - angle, half, camera, focal, cx, cy, w, h, zoom);
    }

    private void face(Graphics2D g2, BufferedImage img, double theta, double half, double camera, double focal,
                      double cx, double cy, int w, int h, double zoom) {
        double cos = Math.cos(theta);
        double sin = Math.sin(theta);
        if (cos <= 0.01) return;
        int strip = 2;
        double dark = (1 - cos) * 0.55;
        Color shade = Theme.alpha(Color.BLACK, dark);
        for (int sx = 0; sx < w; sx += strip) {
            double u0 = sx - half;
            double u1 = Math.min(w, sx + strip) - half;
            double x0 = u0 * cos + half * sin;
            double z0 = -u0 * sin + half * cos;
            double x1 = u1 * cos + half * sin;
            double z1 = -u1 * sin + half * cos;
            double s0 = focal / (camera - z0) * zoom;
            double s1 = focal / (camera - z1) * zoom;
            double px0 = cx + x0 * s0;
            double px1 = cx + x1 * s1;
            if (px1 <= px0) continue;
            double hs = (s0 + s1) / 2;
            double top = cy - h / 2.0 * hs;
            double bh = h * hs;
            int ix0 = (int) Math.round(px0);
            int ix1 = (int) Math.round(px1);
            if (ix1 <= ix0) continue;
            int iy0 = (int) Math.round(top);
            int iy1 = (int) Math.round(top + bh);
            g2.drawImage(img, ix0, iy0, ix1, iy1, sx, 0, Math.min(w, sx + strip), h, null);
            if (dark > 0.01) {
                g2.setColor(shade);
                g2.fillRect(ix0, iy0, ix1 - ix0, iy1 - iy0);
            }
        }
    }
}
