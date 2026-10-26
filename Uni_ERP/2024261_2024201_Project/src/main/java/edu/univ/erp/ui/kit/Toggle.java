package edu.univ.erp.ui.kit;

import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;
import edu.univ.erp.ui.theme.Theme;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** An on/off switch whose knob springs across. */
public class Toggle extends JComponent {

    private boolean on;
    private double pos;
    private Animator.Anim anim;
    private final List<Consumer<Boolean>> listeners = new ArrayList<>();

    public Toggle(boolean on) {
        this.on = on;
        this.pos = on ? 1 : 0;
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) setOn(!Toggle.this.on, true);
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE && isEnabled()) setOn(!Toggle.this.on, true);
            }
        });
    }

    public Toggle onChange(Consumer<Boolean> l) {
        listeners.add(l);
        return this;
    }

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean value, boolean notify) {
        if (value == on) return;
        on = value;
        if (anim != null) anim.cancel();
        double from = pos;
        double target = on ? 1 : 0;
        anim = Animator.animate(320, Easing.OUT_BACK, t -> {
            pos = Animator.lerp(from, target, t);
            repaint();
        }, null);
        if (notify) listeners.forEach(l -> l.accept(on));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(44, 26);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        Theme.Palette p = Theme.p();
        int w = 44;
        int h = 24;
        int y = (getHeight() - h) / 2;
        double k = Math.max(0, Math.min(1, pos));
        g2.setColor(Theme.mix(Theme.isDark() ? p.border() : Theme.hex("#D5D9E6"), p.accent(), k));
        if (!isEnabled()) g2.setColor(Theme.alpha(g2.getColor(), 0.5));
        g2.fill(new RoundRectangle2D.Double(0, y, w, h, h, h));
        double kx = 3 + pos * (w - h);
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fill(new Ellipse2D.Double(kx, y + 4, h - 6, h - 6));
        g2.setColor(Color.WHITE);
        g2.fill(new Ellipse2D.Double(kx, y + 3, h - 6, h - 6));
        if (isFocusOwner()) {
            g2.setColor(Theme.alpha(p.accent(), 0.3));
            g2.setStroke(new java.awt.BasicStroke(2f));
            g2.draw(new RoundRectangle2D.Double(-1, y - 1, w + 1, h + 1, h + 2, h + 2));
        }
        g2.dispose();
    }
}
