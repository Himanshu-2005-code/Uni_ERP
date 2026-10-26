package edu.univ.erp.ui.chart;

import edu.univ.erp.ui.theme.Theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

/** Spider chart of values in 0..max across named axes; the shape grows outward on entry. */
public class RadarChart extends ChartBase {

    private List<String> axes = List.of();
    private final List<double[]> series = new ArrayList<>();
    private final List<Color> colors = new ArrayList<>();
    private double max = 100;

    public RadarChart() {
        duration(1100);
    }

    public RadarChart axes(List<String> a, double max) {
        axes = a;
        this.max = max;
        series.clear();
        colors.clear();
        return this;
    }

    public RadarChart addSeries(double[] values, Color c) {
        series.add(values);
        colors.add(c);
        dataChanged();
        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(300, 260);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        Theme.Palette p = Theme.p();
        int n = axes.size();
        if (n < 3) {
            g2.dispose();
            return;
        }
        double cx = getWidth() / 2.0;
        double cy = getHeight() / 2.0 + 4;
        double r = Math.min(getWidth() - 120, getHeight() - 50) / 2.0;
        for (int ring = 1; ring <= 4; ring++) {
            Path2D poly = new Path2D.Double();
            for (int i = 0; i < n; i++) {
                double a = -Math.PI / 2 + 2 * Math.PI * i / n;
                double rr = r * ring / 4;
                if (i == 0) poly.moveTo(cx + Math.cos(a) * rr, cy + Math.sin(a) * rr);
                else poly.lineTo(cx + Math.cos(a) * rr, cy + Math.sin(a) * rr);
            }
            poly.closePath();
            g2.setColor(Theme.alpha(p.border(), ring == 4 ? 1 : 0.7));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(poly);
        }
        g2.setFont(Theme.medium(11.5f));
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < n; i++) {
            double a = -Math.PI / 2 + 2 * Math.PI * i / n;
            g2.setColor(Theme.alpha(p.border(), 0.8));
            g2.drawLine((int) cx, (int) cy, (int) (cx + Math.cos(a) * r), (int) (cy + Math.sin(a) * r));
            String s = ellipsize(axes.get(i), fm, 90);
            double lx = cx + Math.cos(a) * (r + 14);
            double ly = cy + Math.sin(a) * (r + 14);
            double tx = Math.cos(a) > 0.2 ? lx : Math.cos(a) < -0.2 ? lx - fm.stringWidth(s) : lx - fm.stringWidth(s) / 2.0;
            g2.setColor(p.muted());
            g2.drawString(s, (float) tx, (float) (ly + fm.getAscent() / 2.0 - 2));
        }
        double e = progress;
        for (int s = 0; s < series.size(); s++) {
            double[] v = series.get(s);
            Path2D poly = new Path2D.Double();
            List<double[]> pts = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                double a = -Math.PI / 2 + 2 * Math.PI * i / n;
                double rr = r * Math.min(1, v[i] / max) * e;
                double x = cx + Math.cos(a) * rr;
                double y = cy + Math.sin(a) * rr;
                pts.add(new double[]{x, y});
                if (i == 0) poly.moveTo(x, y);
                else poly.lineTo(x, y);
            }
            poly.closePath();
            Color c = colors.get(s);
            g2.setColor(Theme.alpha(c, 0.22));
            g2.fill(poly);
            g2.setColor(c);
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(poly);
            for (double[] pt : pts) {
                g2.setColor(p.surface());
                g2.fill(new Ellipse2D.Double(pt[0] - 3.5, pt[1] - 3.5, 7, 7));
                g2.setColor(c);
                g2.draw(new Ellipse2D.Double(pt[0] - 3.5, pt[1] - 3.5, 7, 7));
            }
        }
        g2.dispose();
    }
}
