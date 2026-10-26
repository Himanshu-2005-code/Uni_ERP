package edu.univ.erp.ui.kit;

import edu.univ.erp.ui.theme.Theme;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** The product mark: a gradient tile with a graduation cap. */
public final class Brand {

    public static final String NAME = "Campus ERP";

    private Brand() {
    }

    public static void paintMark(Graphics2D g2, double x, double y, double size) {
        g2.setPaint(new GradientPaint((float) x, (float) y, Theme.hex("#818CF8"), (float) (x + size), (float) (y + size),
                Theme.hex("#06B6D4")));
        g2.fill(new RoundRectangle2D.Double(x, y, size, size, size * 0.56, size * 0.56));
        g2.setPaint(new GradientPaint((float) x, (float) y, new Color(255, 255, 255, 70), (float) x,
                (float) (y + size * 0.6), new Color(255, 255, 255, 0)));
        g2.fill(new RoundRectangle2D.Double(x + 1, y + 1, size - 2, size * 0.55, size * 0.5, size * 0.5));
        Icons.paint(g2, "cap", x + size * 0.18, y + size * 0.16, size * 0.64, Color.WHITE, (float) Math.max(1.6, size / 16));
    }

    /** Window icons at several sizes. */
    public static List<Image> icons() {
        List<Image> out = new ArrayList<>();
        for (int s : new int[]{16, 24, 32, 48, 64, 128, 256}) {
            BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = Theme.hq(img.getGraphics());
            paintMark(g, 0, 0, s);
            g.dispose();
            out.add(img);
        }
        return out;
    }
}
