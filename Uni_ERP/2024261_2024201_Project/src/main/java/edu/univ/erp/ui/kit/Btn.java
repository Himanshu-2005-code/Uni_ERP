package edu.univ.erp.ui.kit;

import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;
import edu.univ.erp.ui.theme.Theme;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;

/** A button with animated hover/press states, an optional icon and a loading spinner. */
public class Btn extends JButton {

    public enum Kind { PRIMARY, SECONDARY, GHOST, DANGER, SUCCESS }

    private final Kind kind;
    private String iconName;
    private double hover;
    private double press;
    private boolean loading;
    private double spin;
    private final Runnable spinner = () -> {
        spin += 0.18;
        repaint();
    };
    private Animator.Anim hoverAnim;
    private Animator.Anim pressAnim;
    private boolean compact;

    public Btn(String text, Kind kind) {
        this(text, null, kind);
    }

    public Btn(String text, String icon, Kind kind) {
        super(text);
        this.kind = kind;
        this.iconName = icon;
        setFont(Theme.semibold(13f));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                animateHover(1);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                animateHover(0);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                animatePress(1);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                animatePress(0);
            }
        });
    }

    public static Btn primary(String text, String icon) {
        return new Btn(text, icon, Kind.PRIMARY);
    }

    public static Btn secondary(String text, String icon) {
        return new Btn(text, icon, Kind.SECONDARY);
    }

    public static Btn ghost(String text, String icon) {
        return new Btn(text, icon, Kind.GHOST);
    }

    public static Btn danger(String text, String icon) {
        return new Btn(text, icon, Kind.DANGER);
    }

    /** A square icon-only button with a tooltip. */
    public static Btn icon(String icon, String tooltip) {
        Btn b = new Btn(null, icon, Kind.GHOST);
        b.setToolTipText(tooltip);
        b.compact = true;
        return b;
    }

    public Kind kind() {
        return kind;
    }

    public Btn compact() {
        compact = true;
        return this;
    }

    public void setIconName(String name) {
        this.iconName = name;
        repaint();
    }

    private void animateHover(double target) {
        if (hoverAnim != null) hoverAnim.cancel();
        double from = hover;
        hoverAnim = Animator.animate(180, Easing.OUT_CUBIC, t -> {
            hover = Animator.lerp(from, target, t);
            repaint();
        }, null);
    }

    private void animatePress(double target) {
        if (pressAnim != null) pressAnim.cancel();
        double from = press;
        pressAnim = Animator.animate(target > 0 ? 90 : 260, target > 0 ? Easing.OUT_CUBIC : Easing.OUT_BACK, t -> {
            press = Animator.lerp(from, target, t);
            repaint();
        }, null);
    }

    public void setLoading(boolean on) {
        if (on == loading) return;
        loading = on;
        setEnabled(!on);
        if (on) Animator.addLoop(spinner);
        else Animator.removeLoop(spinner);
        repaint();
    }

    public boolean isLoading() {
        return loading;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        String t = getText();
        boolean hasText = t != null && !t.isEmpty();
        int iconW = iconName != null ? 18 : 0;
        int gap = hasText && iconName != null ? 8 : 0;
        int w = (hasText ? fm.stringWidth(t) : 0) + iconW + gap;
        int h = compact ? 34 : 40;
        if (!hasText) return new Dimension(h, h);
        Insets in = getInsets();
        return new Dimension(w + (compact ? 24 : 36) + in.left + in.right - 2 * Math.min(in.left, 2), h);
    }

    @Override
    public Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        int w = getWidth();
        int h = getHeight();
        double scale = 1 - press * 0.035;
        g2.translate(w / 2.0, h / 2.0);
        g2.scale(scale, scale);
        g2.translate(-w / 2.0, -h / 2.0);
        boolean enabled = isEnabled() || loading;
        float arc = Math.min(h, 24);
        RoundRectangle2D shape = new RoundRectangle2D.Double(1, 1, w - 2, h - 2, arc, arc);
        Theme.Palette p = Theme.p();
        Color fg;
        switch (kind) {
            case PRIMARY, DANGER, SUCCESS -> {
                Color base = kind == Kind.PRIMARY ? p.accent() : kind == Kind.DANGER ? p.danger() : p.success();
                Color top = Theme.brighter(base, 0.08 + hover * 0.08);
                Color bottom = Theme.darker(base, 0.08 - hover * 0.04);
                if (hover > 0.01 && enabled) {
                    for (int i = 6; i >= 1; i--) {
                        g2.setColor(Theme.alpha(base, 0.05 * hover * (7 - i) / 6.0));
                        g2.fill(new RoundRectangle2D.Double(1 - i, 3 - i * 0.4, w - 2 + 2 * i, h - 2 + 2 * i, arc + i * 2, arc + i * 2));
                    }
                }
                g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
                if (!enabled) g2.setComposite(java.awt.AlphaComposite.SrcOver.derive(0.5f));
                g2.fill(shape);
                g2.setColor(Theme.alpha(Color.WHITE, 0.18));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(1.5, 1.5, w - 3, h - 3, arc, arc));
                fg = Color.WHITE;
            }
            case SECONDARY -> {
                g2.setColor(Theme.mix(Theme.isDark() ? p.surfaceAlt() : p.surface(), p.hover(), hover));
                g2.fill(shape);
                g2.setColor(Theme.mix(Theme.mix(p.border(), p.muted(), 0.22), p.accent(), hover * 0.5));
                g2.setStroke(new BasicStroke(1.1f));
                g2.draw(shape);
                fg = enabled ? p.text() : p.subtle();
            }
            default -> {
                if (hover > 0.01) {
                    g2.setColor(Theme.alpha(Theme.isDark() ? Color.WHITE : p.accent(), 0.08 * hover));
                    g2.fill(shape);
                }
                fg = enabled ? Theme.mix(p.muted(), p.text(), hover) : p.subtle();
            }
        }
        if (getForeground() != null && kind == Kind.GHOST && getClientProperty("fg") instanceof Color c) fg = c;

        String t = getText();
        boolean hasText = t != null && !t.isEmpty();
        FontMetrics fm = g2.getFontMetrics(getFont());
        int iconSize = compact && !hasText ? 18 : 17;
        int contentW = (hasText ? fm.stringWidth(t) : 0) + (iconName != null || loading ? iconSize : 0)
                + (hasText && (iconName != null || loading) ? 8 : 0);
        int x = (w - contentW) / 2;
        if (loading) {
            g2.setColor(fg);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double cy = h / 2.0;
            g2.draw(new Arc2D.Double(x + 1.5, cy - iconSize / 2.0 + 1.5, iconSize - 3, iconSize - 3,
                    -Math.toDegrees(spin), 270, Arc2D.OPEN));
            x += iconSize + (hasText ? 8 : 0);
        } else if (iconName != null) {
            Icons.paint(g2, iconName, x, (h - iconSize) / 2.0, iconSize, fg, 1.8f);
            x += iconSize + (hasText ? 8 : 0);
        }
        if (hasText) {
            g2.setFont(getFont());
            g2.setColor(fg);
            g2.drawString(t, x, (h - fm.getHeight()) / 2 + fm.getAscent());
        }
        if (isFocusOwner() && !compact) {
            g2.setColor(Theme.alpha(p.accent(), 0.45));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new RoundRectangle2D.Double(-1, -1, w + 1, h + 1, arc + 4, arc + 4));
        }
        g2.dispose();
    }

    @Override
    public void removeNotify() {
        Animator.removeLoop(spinner);
        super.removeNotify();
    }
}
