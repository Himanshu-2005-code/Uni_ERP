package edu.univ.erp.ui.kit;

import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;
import edu.univ.erp.ui.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.HierarchyEvent;
import java.util.function.DoubleFunction;

/** A KPI tile: icon, caption, a number that counts up when first shown, and a note. */
public class StatCard extends Card {

    private final Number number;
    private final JLabel note;

    public StatCard(String icon, Color color, String caption, double value, DoubleFunction<String> format, String noteText) {
        super(new MigLayout("insets 0, fillx, wrap 1", "[grow]", "[]14[]2[]"));
        hoverLift();
        JLabel ic = new JLabel(new IconBubble(icon, 40, color));
        add(ic);
        number = new Number(value, format);
        add(number, "growx");
        JLabel cap = UI.muted(caption);
        cap.setFont(Theme.medium(12.5f));
        add(cap);
        note = UI.muted(noteText == null ? "" : noteText);
        note.setFont(Theme.font(11.5f));
        if (noteText != null) add(note, "gaptop 4");
    }

    public void setValue(double value) {
        number.animateTo(value);
    }

    public void setNote(String text) {
        note.setText(text);
    }

    private static final class Number extends JComponent {
        private final DoubleFunction<String> format;
        private double target;
        private double shown;
        private boolean started;
        private Animator.Anim anim;

        Number(double target, DoubleFunction<String> format) {
            this.target = target;
            this.format = format;
            setFont(Theme.semibold(28f));
            addHierarchyListener(e -> {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing() && !started) {
                    started = true;
                    run(0, this.target);
                }
            });
        }

        void animateTo(double v) {
            target = v;
            if (started) run(shown, v);
            else repaint();
        }

        private void run(double from, double to) {
            if (anim != null) anim.cancel();
            anim = Animator.animate(1100, Easing.OUT_EXPO, t -> {
                shown = Animator.lerp(from, to, t);
                repaint();
            }, null);
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(format.apply(target)) + 4, fm.getHeight());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.hq(g);
            g2.setFont(getFont());
            g2.setColor(Theme.p().text());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(format.apply(started ? shown : 0), 0, fm.getAscent());
            g2.dispose();
        }
    }
}
