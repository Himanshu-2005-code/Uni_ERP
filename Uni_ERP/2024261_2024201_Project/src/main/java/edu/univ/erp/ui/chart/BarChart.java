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
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;

/** Vertical bars, one or more series per category, rising in a stagger on entry. */
public class BarChart extends ChartBase {

    private List<String> labels = List.of();
    private final List<double[]> series = new ArrayList<>();
    private final List<String> names = new ArrayList<>();
    private final List<Color> colors = new ArrayList<>();
    private DoubleFunction<String> fmt = ChartBase::compact;
    private int hover = -1;
    private Double fixedMax;
    private boolean colorByCategory;

    public BarChart() {
        duration(1100);
    }

    public BarChart format(DoubleFunction<String> f) {
        fmt = f;
        return this;
    }

    public BarChart max(double m) {
        fixedMax = m;
        return this;
    }

    /** Colours each bar by its category instead of by series. */
    public BarChart colorByCategory() {
        colorByCategory = true;
        return this;
    }

    public BarChart data(List<String> categoryLabels, String name, double[] values, Color color) {
        labels = categoryLabels;
        series.clear();
        names.clear();
        colors.clear();
        return addSeries(name, values, color);
    }

    public BarChart addSeries(String name, double[] values, Color color) {
        series.add(values);
        names.add(name);
        colors.add(color);
        dataChanged();
        return this;
    }

    private double max() {
        if (fixedMax != null) return fixedMax;
        double m = 0;
        for (double[] s : series) for (double v : s) m = Math.max(m, v);
        double step = niceStep(m, 4);
        return Math.max(step, Math.ceil(m / step) * step);
    }

    private int left() {
        FontMetrics fm = getFontMetrics(getFont());
        return fm.stringWidth(fmt.apply(max())) + 14;
    }

    @Override
    protected void onHover(Point p) {
        if (p == null || labels.isEmpty()) {
            hover = -1;
            return;
        }
        int l = left();
        double slot = (getWidth() - l - 8) / (double) labels.size();
        int i = (int) ((p.x - l) / slot);
        hover = p.x < l || i >= labels.size() || p.y > getHeight() - 26 ? -1 : i;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(420, 260);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        Theme.Palette p = Theme.p();
        int w = getWidth();
        int h = getHeight();
        int l = left();
        int top = 18;
        int bottom = h - 28;
        double max = max();
        double step = niceStep(max, 4);
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        for (double v = 0; v <= max + 1e-9; v += step) {
            int y = (int) (bottom - (bottom - top) * v / max);
            g2.setColor(Theme.alpha(p.border(), v == 0 ? 1 : 0.7));
            g2.setStroke(v == 0 ? new BasicStroke(1f) : new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10, new float[]{3, 4}, 0));
            g2.drawLine(l, y, w - 8, y);
            g2.setColor(p.subtle());
            String s = fmt.apply(v);
            g2.drawString(s, l - 10 - fm.stringWidth(s), y + 4);
        }
        if (labels.isEmpty()) {
            g2.dispose();
            return;
        }
        int n = labels.size();
        double slot = (w - l - 8) / (double) n;
        int ns = series.size();
        double groupW = Math.min(slot * 0.62, 26.0 * ns + 6 * (ns - 1));
        double barW = (groupW - 6 * (ns - 1)) / ns;
        for (int i = 0; i < n; i++) {
            double cx = l + slot * i + slot / 2;
            double stagger = Math.max(0, Math.min(1, (progress * (1 + n * 0.06) - i * 0.06)));
            double e = 1 - Math.pow(1 - stagger, 3);
            boolean dim = hover >= 0 && hover != i;
            if (hover == i) {
                g2.setColor(Theme.alpha(p.accent(), Theme.isDark() ? 0.08 : 0.05));
                g2.fillRoundRect((int) (l + slot * i + 2), top - 6, (int) slot - 4, bottom - top + 6, 12, 12);
            }
            for (int s = 0; s < ns; s++) {
                double v = series.get(s)[i];
                double bh = (bottom - top) * v / max * e;
                double x = cx - groupW / 2 + s * (barW + 6);
                Color c = colorByCategory ? Theme.series(i) : colors.get(s);
                if (dim) c = Theme.alpha(c, 0.45);
                g2.setPaint(new GradientPaint((float) x, (float) (bottom - bh), Theme.brighter(c, 0.12), (float) x, bottom,
                        Theme.darker(c, 0.05)));
                g2.fill(roundTop(x, bottom - bh, barW, bh, Math.min(8, barW / 2)));
                if (progress > 0.9 && v > 0 && (ns == 1 && n <= 12 || hover == i)) {
                    g2.setFont(Theme.semibold(11f));
                    String vs = fmt.apply(v);
                    int tw = g2.getFontMetrics().stringWidth(vs);
                    g2.setColor(Theme.alpha(p.text(), (progress - 0.9) * 10 * (dim ? 0.4 : 1)));
                    g2.drawString(vs, (float) (x + barW / 2 - tw / 2.0), (float) (bottom - bh - 6));
                    g2.setFont(getFont());
                }
            }
            g2.setColor(hover == i ? p.text() : p.muted());
            String lab = ellipsize(labels.get(i), fm, (int) slot - 4);
            g2.drawString(lab, (float) (cx - fm.stringWidth(lab) / 2.0), h - 8);
        }
        if (hover >= 0 && mouse != null && ns > 1) {
            List<String[]> lines = new ArrayList<>();
            for (int s = 0; s < ns; s++) lines.add(new String[]{names.get(s), fmt.apply(series.get(s)[hover])});
            tooltip(g2, mouse.x, mouse.y, labels.get(hover), lines, colors);
        }
        g2.dispose();
    }

    static Path2D roundTop(double x, double y, double w, double h, double r) {
        Path2D p = new Path2D.Double();
        r = Math.min(r, h);
        p.moveTo(x, y + h);
        p.lineTo(x, y + r);
        p.quadTo(x, y, x + r, y);
        p.lineTo(x + w - r, y);
        p.quadTo(x + w, y, x + w, y + r);
        p.lineTo(x + w, y + h);
        p.closePath();
        return p;
    }
}
