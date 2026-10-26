package edu.univ.erp.ui.chart;

import edu.univ.erp.ui.theme.Theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;

/** A ring gauge with a value in the centre, e.g. CGPA out of 10 or seats used. */
public class Gauge extends ChartBase {

    private double value;
    private double max = 1;
    private String text = "";
    private String caption = "";
    private Color from = Theme.series(0);
    private Color to = Theme.series(1);

    public Gauge() {
        duration(1400);
    }

    public Gauge set(double value, double max, String text, String caption) {
        this.value = value;
        this.max = max <= 0 ? 1 : max;
        this.text = text;
        this.caption = caption;
        dataChanged();
        return this;
    }

    public Gauge colors(Color a, Color b) {
        from = a;
        to = b;
        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(180, 180);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        Theme.Palette p = Theme.p();
        double size = Math.min(getWidth(), getHeight()) - 12;
        double x = (getWidth() - size) / 2;
        double y = (getHeight() - size) / 2;
        float th = (float) Math.max(10, size * 0.09);
        Arc2D track = new Arc2D.Double(x + th / 2, y + th / 2, size - th, size - th, 225, -270, Arc2D.OPEN);
        g2.setStroke(new BasicStroke(th, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.isDark() ? p.surfaceAlt() : Theme.hex("#EEF0F6"));
        g2.draw(track);
        double frac = Math.max(0, Math.min(1, value / max)) * progress;
        if (frac > 0.001) {
            g2.setPaint(new GradientPaint((float) x, (float) (y + size), from, (float) (x + size), (float) y, to));
            g2.draw(new Arc2D.Double(x + th / 2, y + th / 2, size - th, size - th, 225, -270 * frac, Arc2D.OPEN));
        }
        g2.setFont(Theme.semibold((float) (size * 0.2)));
        FontMetrics fb = g2.getFontMetrics();
        g2.setColor(p.text());
        g2.drawString(text, (float) (getWidth() / 2.0 - fb.stringWidth(text) / 2.0), (float) (getHeight() / 2.0 + fb.getAscent() / 2.5));
        g2.setFont(Theme.font(11.5f));
        FontMetrics fs = g2.getFontMetrics();
        g2.setColor(p.muted());
        g2.drawString(caption, (float) (getWidth() / 2.0 - fs.stringWidth(caption) / 2.0), (float) (getHeight() / 2.0 + fb.getAscent() / 2.5 + 18));
        g2.dispose();
    }
}
