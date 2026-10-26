package edu.univ.erp.ui.kit;

import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.theme.Theme;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** Shimmering placeholder blocks shown while a page loads. */
public class Skeleton extends JComponent {

    private final long start = System.nanoTime();
    private final Runnable tick = this::repaint;

    @Override
    public void addNotify() {
        super.addNotify();
        Animator.addLoop(tick);
    }

    @Override
    public void removeNotify() {
        Animator.removeLoop(tick);
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        int w = getWidth();
        Theme.Palette p = Theme.p();
        Color base = Theme.isDark() ? p.surfaceAlt() : Theme.hex("#E9ECF4");
        Color shine = Theme.isDark() ? p.border() : Theme.hex("#F6F7FB");
        double phase = ((System.nanoTime() - start) / 1e9 * 0.8) % 1.0;
        double sx = -w + phase * w * 3;
        g2.setPaint(new GradientPaint((float) sx, 0, base, (float) (sx + w / 2.0), 0, shine, true));
        int gap = 20;
        int cw = (w - gap * 3) / 4;
        for (int i = 0; i < 4; i++) {
            g2.fill(new RoundRectangle2D.Double(i * (cw + gap), 0, cw, 128, 22, 22));
        }
        g2.fill(new RoundRectangle2D.Double(0, 150, w * 0.62, 300, 22, 22));
        g2.fill(new RoundRectangle2D.Double(w * 0.62 + gap, 150, w * 0.38 - gap, 300, 22, 22));
        g2.fill(new RoundRectangle2D.Double(0, 472, w, 200, 22, 22));
        g2.dispose();
    }
}
