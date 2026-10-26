package edu.univ.erp.ui.kit;

import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.HierarchyEvent;

/** Wraps a component so it fades and rises into place, after a delay, the first time it's shown. */
public class FadeIn extends JPanel {

    private float alpha;
    private double dy = 16;
    private boolean played;

    public FadeIn(JComponent child, int delayMs) {
        super(new BorderLayout());
        setOpaque(false);
        add(child);
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing() && !played) {
                played = true;
                Timer t = new Timer(Math.max(1, delayMs), ev -> Animator.animate(520, Easing.OUT_CUBIC, v -> {
                    alpha = (float) v;
                    dy = 16 * (1 - v);
                    repaint();
                }, null));
                t.setRepeats(false);
                t.start();
            }
        });
    }

    public static FadeIn of(JComponent c, int index) {
        return new FadeIn(c, 40 + Math.min(index, 12) * 55);
    }

    @Override
    public void paint(Graphics g) {
        if (alpha >= 1f) {
            super.paint(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0, alpha)));
        g2.translate(0, dy);
        super.paint(g2);
        g2.dispose();
    }
}
