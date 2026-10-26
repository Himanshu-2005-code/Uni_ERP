package edu.univ.erp.ui.chart;

import edu.univ.erp.ui.theme.Theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;

/** Smooth line chart with gradient area fill, drawn left to right on entry, with a hover crosshair. */
public class LineChart extends ChartBase {

    private List<String> labels = List.of();
    private final List<double[]> series = new ArrayList<>();
    private final List<String> names = new ArrayList<>();
    private final List<Color> colors = new ArrayList<>();
    private DoubleFunction<String> fmt = ChartBase::compact;
    private Double fixedMin;
    private Double fixedMax;
    private int hover = -1;
    private int labelEvery = 1;
    private boolean area = true;

    public LineChart() {
        duration(1300);
    }

    public LineChart data(List<String> labels, String name, double[] values, Color color) {
        this.labels = labels;
        series.clear();
        names.clear();
        colors.clear();
        return addSeries(name, values, color);
    }

    public LineChart addSeries(String name, double[] values, Color color) {
        series.add(values);
        names.add(name);
        colors.add(color);
        dataChanged();
        return this;
    }

    public LineChart range(double min, double max) {
        fixedMin = min;
        fixedMax = max;
        return this;
    }

    public LineChart format(DoubleFunction<String> f) {
        fmt = f;
        return this;
    }

    public LineChart labelEvery(int n) {
        labelEvery = Math.max(1, n);
        return this;
    }

    public LineChart noArea() {
        area = false;
        return this;
    }

    private double[] valueRange() {
        double lo = Double.MAX_VALUE;
        double hi = -Double.MAX_VALUE;
        for (double[] s : series) for (double v : s) {
            lo = Math.min(lo, v);
            hi = Math.max(hi, v);
        }
        if (fixedMin != null) lo = fixedMin;
        if (fixedMax != null) hi = fixedMax;
        if (lo == Double.MAX_VALUE) return new double[]{0, 1};
        if (fixedMin == null) lo = Math.min(0, lo);
        if (fixedMax == null) {
            double step = niceStep(hi - lo, 4);
            hi = Math.max(lo + step, Math.ceil(hi / step) * step);
        }
        if (hi <= lo) hi = lo + 1;
        return new double[]{lo, hi};
    }

    private int left() {
        double[] b = valueRange();
        FontMetrics fm = getFontMetrics(getFont());
        return Math.max(fm.stringWidth(fmt.apply(b[1])), fm.stringWidth(fmt.apply(b[0]))) + 14;
    }

    private double xAt(int i, int l, int w) {
        int n = labels.size();
        return n <= 1 ? l + (w - l - 12) / 2.0 : l + 6 + (w - l - 18) * i / (double) (n - 1);
    }

    @Override
    protected void onHover(Point p) {
        if (p == null || labels.isEmpty()) {
            hover = -1;
            return;
        }
        int l = left();
        int best = -1;
        double bd = Double.MAX_VALUE;
        for (int i = 0; i < labels.size(); i++) {
            double d = Math.abs(xAt(i, l, getWidth()) - p.x);
            if (d < bd) {
                bd = d;
                best = i;
            }
        }
        hover = p.x < l - 4 ? -1 : best;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(480, 260);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        Theme.Palette p = Theme.p();
        int w = getWidth();
        int h = getHeight();
        int l = left();
        int top = 16;
        int bottom = h - 28;
        double[] b = valueRange();
        double span = b[1] - b[0];
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        double step = niceStep(span, 4);
        for (double v = b[0]; v <= b[1] + 1e-9; v += step) {
            int y = (int) (bottom - (bottom - top) * (v - b[0]) / span);
            g2.setColor(Theme.alpha(p.border(), 0.75));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{3, 4}, 0));
            g2.drawLine(l, y, w - 8, y);
            g2.setColor(p.subtle());
            String s = fmt.apply(v);
            g2.drawString(s, l - 10 - fm.stringWidth(s), y + 4);
        }
        int n = labels.size();
        for (int i = 0; i < n; i += labelEvery) {
            String s = labels.get(i);
            double x = xAt(i, l, w);
            g2.setColor(hover == i ? p.text() : p.subtle());
            double lx = Math.max(2, Math.min(w - fm.stringWidth(s) - 2, x - fm.stringWidth(s) / 2.0));
            g2.drawString(s, (float) lx, h - 8);
        }
        if (n == 0) {
            g2.dispose();
            return;
        }
        // reveal left to right
        Graphics2D clip = (Graphics2D) g2.create();
        clip.clip(new Rectangle(0, 0, (int) (l + (w - l) * progress) + 6, h));
        for (int s = series.size() - 1; s >= 0; s--) {
            double[] vals = series.get(s);
            double[] xs = new double[n];
            double[] ys = new double[n];
            for (int i = 0; i < n; i++) {
                xs[i] = xAt(i, l, w);
                ys[i] = bottom - (bottom - top) * (vals[i] - b[0]) / span;
            }
            Path2D line = smooth(xs, ys);
            Color c = colors.get(s);
            if (area && (s == 0 || series.size() <= 2)) {
                Path2D fill = new Path2D.Double(line);
                fill.lineTo(xs[n - 1], bottom);
                fill.lineTo(xs[0], bottom);
                fill.closePath();
                clip.setPaint(new GradientPaint(0, top, Theme.alpha(c, Theme.isDark() ? 0.35 : 0.25), 0, bottom,
                        Theme.alpha(c, 0)));
                clip.fill(fill);
            }
            clip.setColor(Theme.alpha(c, 0.25));
            clip.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            clip.draw(line);
            clip.setColor(c);
            clip.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            clip.draw(line);
            if (n <= 14) {
                for (int i = 0; i < n; i++) {
                    clip.setColor(p.surface());
                    clip.fill(new Ellipse2D.Double(xs[i] - 4, ys[i] - 4, 8, 8));
                    clip.setColor(c);
                    clip.setStroke(new BasicStroke(2f));
                    clip.draw(new Ellipse2D.Double(xs[i] - 4, ys[i] - 4, 8, 8));
                }
            }
        }
        clip.dispose();
        if (hover >= 0 && progress > 0.95) {
            double x = xAt(hover, l, w);
            g2.setColor(Theme.alpha(p.muted(), 0.5));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{4, 4}, 0));
            g2.drawLine((int) x, top, (int) x, bottom);
            List<String[]> lines = new ArrayList<>();
            for (int s = 0; s < series.size(); s++) {
                double y = bottom - (bottom - top) * (series.get(s)[hover] - b[0]) / span;
                g2.setColor(Theme.alpha(colors.get(s), 0.25));
                g2.fill(new Ellipse2D.Double(x - 9, y - 9, 18, 18));
                g2.setColor(colors.get(s));
                g2.fill(new Ellipse2D.Double(x - 5, y - 5, 10, 10));
                g2.setColor(Color.WHITE);
                g2.fill(new Ellipse2D.Double(x - 2, y - 2, 4, 4));
                lines.add(new String[]{names.get(s), fmt.apply(series.get(s)[hover])});
            }
            tooltip(g2, (int) x, mouse == null ? top + 40 : mouse.y, labels.get(hover), lines, colors);
        }
        g2.dispose();
    }

    /** Monotone cubic interpolation so the curve never overshoots the data. */
    static Path2D smooth(double[] x, double[] y) {
        int n = x.length;
        Path2D p = new Path2D.Double();
        p.moveTo(x[0], y[0]);
        if (n < 3) {
            for (int i = 1; i < n; i++) p.lineTo(x[i], y[i]);
            return p;
        }
        double[] m = new double[n];
        double[] d = new double[n - 1];
        for (int i = 0; i < n - 1; i++) d[i] = (y[i + 1] - y[i]) / (x[i + 1] - x[i]);
        m[0] = d[0];
        m[n - 1] = d[n - 2];
        for (int i = 1; i < n - 1; i++) m[i] = d[i - 1] * d[i] <= 0 ? 0 : (d[i - 1] + d[i]) / 2;
        for (int i = 0; i < n - 1; i++) {
            if (d[i] == 0) {
                m[i] = 0;
                m[i + 1] = 0;
                continue;
            }
            double a = m[i] / d[i];
            double b = m[i + 1] / d[i];
            double s = a * a + b * b;
            if (s > 9) {
                double t = 3 / Math.sqrt(s);
                m[i] = t * a * d[i];
                m[i + 1] = t * b * d[i];
            }
        }
        for (int i = 0; i < n - 1; i++) {
            double h = x[i + 1] - x[i];
            p.curveTo(x[i] + h / 3, y[i] + m[i] * h / 3, x[i + 1] - h / 3, y[i + 1] - m[i + 1] * h / 3, x[i + 1], y[i + 1]);
        }
        return p;
    }
}
