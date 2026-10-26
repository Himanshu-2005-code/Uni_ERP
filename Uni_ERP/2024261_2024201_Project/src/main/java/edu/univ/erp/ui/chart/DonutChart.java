package edu.univ.erp.ui.chart;

import edu.univ.erp.ui.theme.Theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Arc2D;
import java.util.List;

/** Donut with an animated sweep; the hovered slice pops out and its value shows in the middle. */
public class DonutChart extends ChartBase {

    private List<String> labels = List.of();
    private double[] values = new double[0];
    private Color[] colors;
    private String centerCaption = "Total";
    private int hover = -1;
    private boolean legend = true;

    public DonutChart() {
        duration(1200);
    }

    public DonutChart data(List<String> labels, double[] values) {
        this.labels = labels;
        this.values = values;
        dataChanged();
        return this;
    }

    public DonutChart colors(Color... c) {
        colors = c;
        return this;
    }

    public DonutChart caption(String c) {
        centerCaption = c;
        return this;
    }

    public DonutChart noLegend() {
        legend = false;
        return this;
    }

    private Color color(int i) {
        return colors != null && i < colors.length ? colors[i] : Theme.series(i);
    }

    private double total() {
        double t = 0;
        for (double v : values) t += v;
        return t;
    }

    private double[] geom() {
        int w = getWidth();
        int h = getHeight();
        double size = Math.min(legend ? w * 0.52 : w, h) - 16;
        double cx = legend ? size / 2 + 8 : w / 2.0;
        return new double[]{cx, h / 2.0, size / 2};
    }

    @Override
    protected void onHover(Point p) {
        hover = -1;
        if (p == null) return;
        double[] gm = geom();
        double dx = p.x - gm[0];
        double dy = p.y - gm[1];
        double r = Math.hypot(dx, dy);
        if (r > gm[2] + 8 || r < gm[2] * 0.58) {
            if (legend) legendHover(p);
            return;
        }
        double ang = Math.toDegrees(Math.atan2(-dy, dx));
        double fromTop = (90 - ang + 360) % 360;
        double t = total();
        double acc = 0;
        for (int i = 0; i < values.length; i++) {
            acc += values[i] / t * 360;
            if (fromTop <= acc) {
                hover = i;
                return;
            }
        }
    }

    private void legendHover(Point p) {
        double[] gm = geom();
        int lx = (int) (gm[0] + gm[2] + 28);
        int ly = (int) (getHeight() / 2.0 - values.length * 13);
        if (p.x < lx) return;
        int i = (p.y - ly + 4) / 26;
        if (i >= 0 && i < values.length) hover = i;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(360, 220);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        Theme.Palette p = Theme.p();
        double[] gm = geom();
        double cx = gm[0];
        double cy = gm[1];
        double r = gm[2];
        double thick = Math.max(14, r * 0.3);
        double t = total();
        g2.setColor(Theme.isDark() ? p.surfaceAlt() : Theme.hex("#F0F2F8"));
        g2.setStroke(new BasicStroke((float) thick, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g2.draw(new Arc2D.Double(cx - r + thick / 2, cy - r + thick / 2, 2 * r - thick, 2 * r - thick, 0, 360, Arc2D.OPEN));
        double start = 90;
        double sweepTotal = 360 * progress;
        double drawn = 0;
        for (int i = 0; i < values.length && t > 0; i++) {
            double sweep = values[i] / t * 360;
            double visible = Math.max(0, Math.min(sweep, sweepTotal - drawn));
            if (visible <= 0) break;
            double pop = hover == i ? 6 : 0;
            double mid = Math.toRadians(start - visible / 2);
            double ox = Math.cos(mid) * pop;
            double oy = -Math.sin(mid) * pop;
            Color c = color(i);
            if (hover >= 0 && hover != i) c = Theme.alpha(c, 0.45);
            g2.setColor(c);
            double th = thick + (hover == i ? 4 : 0);
            g2.setStroke(new BasicStroke((float) th, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            double gap = values.length > 1 ? Math.min(1.6, visible / 3) : 0;
            g2.draw(new Arc2D.Double(cx - r + thick / 2 + ox, cy - r + thick / 2 + oy, 2 * r - thick, 2 * r - thick,
                    start - gap / 2, -(visible - gap), Arc2D.OPEN));
            start -= sweep;
            drawn += sweep;
        }
        String big = hover >= 0 ? compact(values[hover]) : compact(t);
        String small = hover >= 0 ? labels.get(hover) : centerCaption;
        g2.setFont(Theme.semibold((float) Math.max(16, r * 0.34)));
        FontMetrics fb = g2.getFontMetrics();
        g2.setColor(p.text());
        g2.drawString(big, (float) (cx - fb.stringWidth(big) / 2.0), (float) (cy + fb.getAscent() / 2.0 - 6));
        g2.setFont(Theme.font(11.5f));
        FontMetrics fs = g2.getFontMetrics();
        g2.setColor(p.muted());
        String sm = ellipsize(small, fs, (int) (r * 1.1));
        g2.drawString(sm, (float) (cx - fs.stringWidth(sm) / 2.0), (float) (cy + fb.getAscent() / 2.0 + 12));
        if (legend) {
            int lx = (int) (cx + r + 28);
            int ly = (int) (getHeight() / 2.0 - values.length * 13);
            for (int i = 0; i < values.length; i++) {
                int y = ly + i * 26;
                g2.setColor(color(i));
                g2.fillRoundRect(lx, y, 10, 10, 4, 4);
                g2.setFont(hover == i ? Theme.semibold(12f) : Theme.medium(12f));
                g2.setColor(hover == i ? p.text() : Theme.mix(p.text(), p.muted(), 0.35));
                FontMetrics fm = g2.getFontMetrics();
                int avail = getWidth() - lx - 70;
                g2.drawString(ellipsize(labels.get(i), fm, avail), lx + 18, y + 10);
                String pct = t == 0 ? "0%" : String.format("%.0f%%", values[i] / t * 100);
                g2.setColor(p.muted());
                g2.drawString(pct, getWidth() - fm.stringWidth(pct) - 4, y + 10);
            }
        }
        g2.dispose();
    }
}
