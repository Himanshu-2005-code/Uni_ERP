package edu.univ.erp.ui.kit;

import edu.univ.erp.ui.theme.Theme;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** An icon on a soft rounded tile in a tint of its colour. */
public final class IconBubble implements Icon {

    private final String icon;
    private final int size;
    private final Color color;

    public IconBubble(String icon, int size, Color color) {
        this.icon = icon;
        this.size = size;
        this.color = color;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = Theme.hq(g);
        g2.setColor(Theme.alpha(color, Theme.isDark() ? 0.2 : 0.12));
        g2.fill(new RoundRectangle2D.Double(x, y, size, size, size * 0.6, size * 0.6));
        double s = size * 0.5;
        Icons.paint(g2, icon, x + (size - s) / 2, y + (size - s) / 2, s, color, 1.9f);
        g2.dispose();
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
