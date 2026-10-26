package edu.univ.erp.ui.chart;

import edu.univ.erp.ui.theme.Theme;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.DoubleFunction;

/** Ranked horizontal bars with labels on the left and values on the right. */
public class HBarChart extends ChartBase {

    private List<String> labels = List.of();
    private double[] values = new double[0];
    private DoubleFunction<String> fmt = ChartBase::compact;
    private int hover = -1;
    private Double fixedMax;

    public HBarChart data(List<String> labels, double[] values) {
        this.labels = labels;
        this.values = values;
        dataChanged();
        return this;
    }

    public HBarChart format(DoubleFunction<String> f) {
        fmt = f;
        return this;
    }

    public HBarChart max(double m) {
        fixedMax = m;
        return this;
    }

    private int rowH() {
        return labels.isEmpty() ? 30 : Math.max(26, Math.min(40, getHeight() / labels.size()));
    }

    @Override
    protected void onHover(Point p) {
        hover = p == null ? -1 : p.y / rowH();
        if (hover >= labels.size()) hover = -1;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(360, Math.max(120, labels.size() * 34));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        Theme.Palette p = Theme.p();
        g2.setFont(Theme.medium(12f));
        FontMetrics fm = g2.getFontMetrics();
        int labelW = 0;
        for (String s : labels) labelW = Math.min(150, Math.max(labelW, fm.stringWidth(s)));
        labelW += 12;
        int valueW = 0;
        for (double v : values) valueW = Math.max(valueW, fm.stringWidth(fmt.apply(v)));
        valueW += 12;
        double max = fixedMax != null ? fixedMax : 0;
        if (fixedMax == null) for (double v : values) max = Math.max(max, v);
        if (max <= 0) max = 1;
        int rh = rowH();
        int barMax = getWidth() - labelW - valueW;
        for (int i = 0; i < labels.size(); i++) {
            int y = i * rh;
            double stagger = Math.max(0, Math.min(1, progress * (1 + labels.size() * 0.07) - i * 0.07));
            double e = 1 - Math.pow(1 - stagger, 3);
            Color c = Theme.series(i);
            boolean dim = hover >= 0 && hover != i;
            g2.setColor(hover == i ? p.text() : p.muted());
            g2.drawString(ellipsize(labels.get(i), fm, labelW - 12), 0, y + rh / 2 + fm.getAscent() / 2 - 2);
            int bh = Math.min(14, rh - 12);
            int by = y + (rh - bh) / 2;
            g2.setColor(Theme.isDark() ? p.surfaceAlt() : Theme.hex("#F0F2F8"));
            g2.fill(new RoundRectangle2D.Double(labelW, by, barMax, bh, bh, bh));
            double bw = Math.max(bh, barMax * values[i] / max * e);
            Color cc = dim ? Theme.alpha(c, 0.45) : c;
            g2.setPaint(new GradientPaint(labelW, 0, Theme.brighter(cc, 0.15), (float) (labelW + bw), 0, cc));
            g2.fill(new RoundRectangle2D.Double(labelW, by, bw, bh, bh, bh));
            g2.setColor(hover == i ? p.text() : Theme.mix(p.text(), p.muted(), 0.3));
            g2.setFont(Theme.semibold(12f));
            String vs = fmt.apply(values[i] * e);
            g2.drawString(vs, getWidth() - g2.getFontMetrics().stringWidth(vs), y + rh / 2 + fm.getAscent() / 2 - 2);
            g2.setFont(Theme.medium(12f));
        }
        g2.dispose();
    }
}
