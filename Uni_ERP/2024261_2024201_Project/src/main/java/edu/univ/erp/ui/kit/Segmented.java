package edu.univ.erp.ui.kit;

import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;
import edu.univ.erp.ui.theme.Theme;

import javax.swing.JComponent;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** Pill tabs with a sliding selection indicator. */
public class Segmented extends JComponent {

    private final String[] labels;
    private final List<IntConsumer> listeners = new ArrayList<>();
    private int selected;
    private double indicator;
    private double indicatorW;
    private Animator.Anim anim;
    private int hover = -1;

    public Segmented(String... labels) {
        this.labels = labels;
        setFont(Theme.semibold(12.5f));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int i = indexAt(e.getX());
                if (i >= 0) select(i, true);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int i = indexAt(e.getX());
                if (i != hover) {
                    hover = i;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = -1;
                repaint();
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
    }

    public Segmented onChange(IntConsumer l) {
        listeners.add(l);
        return this;
    }

    public int selected() {
        return selected;
    }

    private int[] widths() {
        FontMetrics fm = getFontMetrics(getFont());
        int[] w = new int[labels.length];
        for (int i = 0; i < labels.length; i++) w[i] = fm.stringWidth(labels[i]) + 30;
        return w;
    }

    private int xOf(int index) {
        int[] w = widths();
        int x = 4;
        for (int i = 0; i < index; i++) x += w[i];
        return x;
    }

    private int indexAt(int x) {
        int[] w = widths();
        int acc = 4;
        for (int i = 0; i < w.length; i++) {
            if (x >= acc && x < acc + w[i]) return i;
            acc += w[i];
        }
        return -1;
    }

    public void select(int index, boolean notify) {
        if (index == selected && indicatorW > 0) return;
        selected = index;
        double fx = indicator;
        double fw = indicatorW;
        double tx = xOf(index);
        double tw = widths()[index];
        if (anim != null) anim.cancel();
        if (fw == 0) {
            indicator = tx;
            indicatorW = tw;
            repaint();
        } else {
            anim = Animator.animate(320, Easing.OUT_BACK, t -> {
                indicator = Animator.lerp(fx, tx, t);
                indicatorW = Animator.lerp(fw, tw, t);
                repaint();
            }, null);
        }
        if (notify) listeners.forEach(l -> l.accept(index));
    }

    @Override
    public Dimension getPreferredSize() {
        int w = 8;
        for (int x : widths()) w += x;
        return new Dimension(w, 38);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (indicatorW == 0) {
            indicator = xOf(selected);
            indicatorW = widths()[selected];
        }
        Graphics2D g2 = Theme.hq(g);
        Theme.Palette p = Theme.p();
        int h = getHeight();
        g2.setColor(Theme.isDark() ? p.surfaceAlt() : Theme.hex("#EBEEF6"));
        g2.fill(new RoundRectangle2D.Double(0, 0, getPreferredSize().width, h, h, h));
        g2.setColor(Theme.isDark() ? Theme.p().border() : p.surface());
        g2.fill(new RoundRectangle2D.Double(indicator, 4, indicatorW, h - 8, h - 8, h - 8));
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int[] w = widths();
        int x = 4;
        for (int i = 0; i < labels.length; i++) {
            g2.setColor(i == selected ? p.text() : i == hover ? Theme.mix(p.muted(), p.text(), 0.5) : p.muted());
            g2.drawString(labels[i], x + (w[i] - fm.stringWidth(labels[i])) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
            x += w[i];
        }
        g2.dispose();
    }
}
