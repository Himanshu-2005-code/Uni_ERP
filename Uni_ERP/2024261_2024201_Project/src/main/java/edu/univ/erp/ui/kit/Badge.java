package edu.univ.erp.ui.kit;

import edu.univ.erp.ui.theme.Theme;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/** A small coloured pill label, e.g. ACTIVE / LOCKED or a letter grade. */
public class Badge extends JComponent {

    private String text;
    private Color color;
    private boolean dot;

    public Badge(String text, Color color) {
        this.text = text;
        this.color = color;
        setFont(Theme.semibold(11.5f));
    }

    public Badge dot() {
        dot = true;
        return this;
    }

    public void set(String text, Color color) {
        this.text = text;
        this.color = color;
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.stringWidth(text) + (dot ? 30 : 20), 24);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        paint(g2, text, color, 0, (getHeight() - 22) / 2, dot, getFont());
        g2.dispose();
    }

    /** Paints a badge at (x, y); returns its width. Used by table renderers too. */
    public static int paint(Graphics2D g2, String text, Color color, int x, int y, boolean dot, java.awt.Font font) {
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(text) + (dot ? 28 : 18);
        int h = 22;
        g2.setColor(Theme.alpha(color, Theme.isDark() ? 0.2 : 0.12));
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, h, h));
        int tx = x + 9;
        if (dot) {
            g2.setColor(color);
            g2.fill(new Ellipse2D.Double(x + 9, y + h / 2.0 - 3, 6, 6));
            tx += 10;
        }
        g2.setColor(Theme.isDark() ? Theme.brighter(color, 0.25) : Theme.darker(color, 0.15));
        g2.drawString(text, tx, y + (h - fm.getHeight()) / 2 + fm.getAscent());
        return w;
    }
}
