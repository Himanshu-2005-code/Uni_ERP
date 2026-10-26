package edu.univ.erp.ui.kit;

import edu.univ.erp.ui.theme.Theme;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

/** Circular initials avatar with a gradient picked deterministically from the name. */
public final class Avatar implements Icon {

    private final String name;
    private final int size;

    public Avatar(String name, int size) {
        this.name = name == null || name.isBlank() ? "?" : name.trim();
        this.size = size;
    }

    public static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.replaceAll("^(Dr\\.|Prof\\.|Mr\\.|Ms\\.|Mrs\\.)\\s*", "").trim().split("\\s+");
        if (parts.length == 1 && parts[0].length() >= 2 && parts[0].matches(".*\\d.*")) {
            return parts[0].substring(0, 2).toUpperCase();
        }
        String a = parts[0].substring(0, 1);
        String b = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (a + b).toUpperCase();
    }

    public static Color colorFor(String name) {
        return Theme.series(Math.abs((name == null ? "" : name).hashCode()) % 8);
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = Theme.hq(g);
        paint(g2, name, x, y, size);
        g2.dispose();
    }

    public static void paint(Graphics2D g2, String name, int x, int y, int size) {
        Color base = colorFor(name);
        g2.setPaint(new GradientPaint(x, y, Theme.brighter(base, 0.15), x + size, y + size, Theme.darker(base, 0.15)));
        g2.fill(new Ellipse2D.Double(x, y, size, size));
        g2.setColor(Color.WHITE);
        g2.setFont(Theme.semibold(size * 0.38f));
        FontMetrics fm = g2.getFontMetrics();
        String in = initials(name);
        g2.drawString(in, x + (size - fm.stringWidth(in)) / 2f, y + (size - fm.getHeight()) / 2f + fm.getAscent());
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }
}
