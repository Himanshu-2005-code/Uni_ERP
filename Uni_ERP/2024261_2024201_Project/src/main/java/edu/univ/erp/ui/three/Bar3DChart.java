package edu.univ.erp.ui.three;

import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;
import edu.univ.erp.ui.chart.ChartBase;
import edu.univ.erp.ui.theme.Theme;
import edu.univ.erp.ui.three.Engine3D.Camera;
import edu.univ.erp.ui.three.Engine3D.Face;
import edu.univ.erp.ui.three.Engine3D.V;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;

/**
 * Interactive 3D bar chart: categories along X, series along Z. Bars spring up on entry,
 * the scene slowly orbits when idle, drag rotates, the wheel zooms and hovering a bar
 * shows its value.
 */
public class Bar3DChart extends JComponent {

    private List<String> cols = List.of();
    private List<String> rows = List.of();
    private double[][] values = new double[0][0];
    private DoubleFunction<String> fmt = ChartBase::compact;
    private final Camera cam = new Camera();
    private double grow;
    private boolean played;
    private Point dragFrom;
    private long lastInteraction;
    private int[] hover;
    private Point mouse;
    private List<Engine3D.Drawn> drawn = List.of();
    private final Runnable loop = this::frame;
    private double spinVelocity = 0.0025;
    private Double fixedMax;
    private double userZoom = 1;
    private boolean framed;

    public Bar3DChart() {
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        cam.yaw = -0.62;
        cam.pitch = 0.5;
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    Animator.addLoop(loop);
                    if (!played) play();
                } else {
                    Animator.removeLoop(loop);
                }
            }
        });
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragFrom = e.getPoint();
                lastInteraction = System.currentTimeMillis();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragFrom = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragFrom == null) return;
                cam.yaw += (e.getX() - dragFrom.x) * 0.011;
                cam.pitch = Math.max(0.12, Math.min(1.25, cam.pitch + (e.getY() - dragFrom.y) * 0.008));
                dragFrom = e.getPoint();
                lastInteraction = System.currentTimeMillis();
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                mouse = e.getPoint();
                pick();
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mouse = null;
                hover = null;
                repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (!e.isControlDown()) {
                    // Plain scrolling belongs to the page, not the chart
                    java.awt.Container parent = getParent();
                    if (parent != null) parent.dispatchEvent(javax.swing.SwingUtilities.convertMouseEvent(Bar3DChart.this, e, parent));
                    return;
                }
                userZoom = Math.max(0.6, Math.min(2.2, userZoom * Math.pow(0.92, e.getPreciseWheelRotation())));
                lastInteraction = System.currentTimeMillis();
                repaint();
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
        addMouseWheelListener(m);
    }

    public Bar3DChart format(DoubleFunction<String> f) {
        fmt = f;
        return this;
    }

    public Bar3DChart max(double m) {
        fixedMax = m;
        return this;
    }

    /** values[row][col]; rows become series along the depth axis. */
    public Bar3DChart data(List<String> colLabels, List<String> rowLabels, double[][] values) {
        this.cols = colLabels;
        this.rows = rowLabels;
        this.values = values;
        cam.distance = baseDistance();
        cam.target = new V(0, 1.1, 0);
        framed = false;
        if (isShowing()) play();
        else played = false;
        return this;
    }

    private double baseDistance() {
        return Math.max(cols.size() * 1.25, rows.size() * 1.35) * 1.6 + 6;
    }

    /**
     * Fits the scene into the component: projects the scene's bounding box and eases the
     * camera's screen zoom/offset towards a framing that fills the space below the legend.
     */
    private void frame(int w, int h, int top, double x0, double x1, double z0, double z1, double H) {
        double oldZoom = cam.zoom;
        double oldX = cam.offX;
        double oldY = cam.offY;
        cam.zoom = 1;
        cam.offX = 0;
        cam.offY = 0;
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (double x : new double[]{x0, x1}) {
            for (double z : new double[]{z0, z1}) {
                for (double y : new double[]{0, H}) {
                    double[] s = cam.project(new V(x, y, z));
                    minX = Math.min(minX, s[0]);
                    maxX = Math.max(maxX, s[0]);
                    minY = Math.min(minY, s[1]);
                    maxY = Math.max(maxY, s[1]);
                }
            }
        }
        double availW = w - 70;
        double availH = h - top - 34;
        double fit = Math.min(availW / Math.max(1, maxX - minX), availH / Math.max(1, maxY - minY)) * userZoom;
        double cx = (minX + maxX) / 2;
        double cy = (minY + maxY) / 2;
        double tx = w / 2.0 - 10;
        double ty = top + availH / 2 + 8;
        double zx = tx - (w / 2.0 + (cx - w / 2.0) * fit);
        double zy = ty - (h / 2.0 + (cy - h / 2.0) * fit);
        if (!framed) {
            cam.zoom = fit;
            cam.offX = zx;
            cam.offY = zy;
            framed = true;
        } else {
            cam.zoom = oldZoom + (fit - oldZoom) * 0.18;
            cam.offX = oldX + (zx - oldX) * 0.18;
            cam.offY = oldY + (zy - oldY) * 0.18;
        }
    }

    public void play() {
        played = true;
        Animator.animate(1500, Easing.LINEAR, t -> {
            grow = t;
            repaint();
        }, null);
    }

    private void frame() {
        if (dragFrom == null && System.currentTimeMillis() - lastInteraction > 2500) {
            cam.yaw += spinVelocity;
            repaint();
        }
    }

    private double max() {
        if (fixedMax != null) return fixedMax;
        double m = 0;
        for (double[] r : values) for (double v : r) m = Math.max(m, v);
        return m <= 0 ? 1 : m;
    }

    private void pick() {
        hover = null;
        if (mouse == null) return;
        for (int i = drawn.size() - 1; i >= 0; i--) {
            Engine3D.Drawn d = drawn.get(i);
            if (d.tag() instanceof int[] cell && d.shape().contains(mouse)) {
                hover = cell;
                return;
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(520, 340);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(120, 120);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        Theme.Palette p = Theme.p();
        int w = getWidth();
        int h = getHeight();
        cam.viewport(w, h);
        int nc = cols.size();
        int nr = rows.size();
        if (nc == 0 || nr == 0) {
            g2.dispose();
            return;
        }
        double gapX = 1.25;
        double gapZ = 1.35;
        double x0 = -(nc - 1) * gapX / 2;
        double z0 = -(nr - 1) * gapZ / 2;
        double fw = 0.62;
        double H = 3.4;
        double max = max();
        int legendRows = 1;
        {
            g2.setFont(Theme.medium(11.5f));
            FontMetrics lf = g2.getFontMetrics();
            int lx = 6;
            for (String s : rows) {
                int tw = lf.stringWidth(s) + 32;
                if (lx + tw > w - 6) {
                    lx = 6;
                    legendRows++;
                }
                lx += tw;
            }
        }
        frame(w, h, 10 + legendRows * 22, x0 - gapX * 0.75, -x0 + gapX * 0.75, z0 - gapZ * 0.75, -z0 + gapZ * 0.75, H);

        // Floor plate and grid
        double fx0 = x0 - gapX * 0.75;
        double fx1 = -x0 + gapX * 0.75;
        double fz0 = z0 - gapZ * 0.75;
        double fz1 = -z0 + gapZ * 0.75;
        Path2D floor = new Path2D.Double();
        double[][] corners = {cam.project(new V(fx0, 0, fz0)), cam.project(new V(fx1, 0, fz0)),
                cam.project(new V(fx1, 0, fz1)), cam.project(new V(fx0, 0, fz1))};
        floor.moveTo(corners[0][0], corners[0][1]);
        for (int i = 1; i < 4; i++) floor.lineTo(corners[i][0], corners[i][1]);
        floor.closePath();
        g2.setColor(Theme.isDark() ? Theme.alpha(p.accent(), 0.07) : Theme.alpha(p.accent(), 0.05));
        g2.fill(floor);
        g2.setColor(Theme.alpha(p.border(), 0.9));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(floor);
        g2.setColor(Theme.alpha(p.border(), 0.55));
        for (int i = 0; i <= nc; i++) {
            double x = x0 - gapX / 2 + i * gapX;
            line(g2, new V(x, 0, fz0), new V(x, 0, fz1));
        }
        for (int j = 0; j <= nr; j++) {
            double z = z0 - gapZ / 2 + j * gapZ;
            line(g2, new V(fx0, 0, z), new V(fx1, 0, z));
        }

        // Value scale on the back wall corner
        double step = ChartBase.niceStep(max, 3);
        g2.setFont(Theme.font(10.5f));
        FontMetrics fm = g2.getFontMetrics();
        for (double v = step; v <= max + 1e-9; v += step) {
            double y = H * v / max;
            g2.setColor(Theme.alpha(p.border(), 0.6));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{3, 4}, 0));
            line(g2, new V(fx0, y, fz1), new V(fx1, y, fz1));
            double[] s = cam.project(new V(fx1, y, fz1));
            g2.setColor(p.subtle());
            g2.drawString(fmt.apply(v), (float) s[0] + 6, (float) s[1] + 4);
        }
        g2.setStroke(new BasicStroke(1f));

        // Axis labels on the floor edges
        g2.setFont(Theme.medium(11f));
        fm = g2.getFontMetrics();
        List<java.awt.Rectangle> placed = new ArrayList<>();
        for (int i = 0; i < nc; i++) {
            double[] s = cam.project(new V(x0 + i * gapX, 0, fz0 - 0.45));
            String lab = cols.get(i);
            int lw = fm.stringWidth(lab);
            java.awt.Rectangle r = new java.awt.Rectangle((int) (s[0] - lw / 2.0) - 3, (int) s[1] - 10, lw + 6, 16);
            boolean clash = false;
            for (java.awt.Rectangle q : placed) clash |= q.intersects(r);
            if (clash && !(hover != null && hover[1] == i)) continue;
            placed.add(r);
            g2.setColor(hover != null && hover[1] == i ? p.text() : p.muted());
            g2.drawString(lab, (float) (s[0] - lw / 2.0), (float) s[1] + 4);
        }

        // Bars
        List<Face> faces = new ArrayList<>();
        for (int r = 0; r < nr; r++) {
            for (int c = 0; c < nc; c++) {
                double v = values[r][c];
                double delay = (c + r * 0.6) * 0.045;
                double t = Math.max(0, Math.min(1, (grow - delay) / 0.55));
                double e = Easing.OUT_BACK.apply(t);
                double bh = Math.max(0.02, H * v / max * e);
                double cx = x0 + c * gapX;
                double cz = z0 + r * gapZ;
                Color col = Theme.series(r);
                boolean hot = hover != null && hover[0] == r && hover[1] == c;
                boolean dim = hover != null && !hot;
                Color side = hot ? Theme.brighter(col, 0.25) : dim ? Theme.mix(col, p.surface(), 0.45) : col;
                Color top = Theme.brighter(side, 0.3);
                faces.addAll(Engine3D.box(cx - fw / 2, 0, cz - fw / 2, cx + fw / 2, bh, cz + fw / 2, side, top,
                        new int[]{r, c}));
            }
        }
        drawn = Engine3D.render(g2, faces, cam, new V(-0.5, 0.8, -0.75), 0.66);

        // Legend
        int ly = 6;
        int lx = 6;
        g2.setFont(Theme.medium(11.5f));
        fm = g2.getFontMetrics();
        for (int r = 0; r < nr; r++) {
            String s = rows.get(r);
            int tw = fm.stringWidth(s) + 26;
            if (lx + tw > w - 6) {
                lx = 6;
                ly += 22;
            }
            g2.setColor(Theme.series(r));
            g2.fill(new RoundRectangle2D.Double(lx, ly + 3, 10, 10, 4, 4));
            g2.setColor(hover != null && hover[0] == r ? p.text() : p.muted());
            g2.drawString(s, lx + 16, ly + 12);
            lx += tw + 6;
        }
        g2.setFont(Theme.font(10.5f));
        g2.setColor(p.subtle());
        String hint = "Drag to rotate · Ctrl + scroll to zoom";
        g2.drawString(hint, w - g2.getFontMetrics().stringWidth(hint) - 6, h - 6);

        if (hover != null && mouse != null) {
            new Tip().draw(g2, mouse.x, mouse.y, cols.get(hover[1]) + " · " + rows.get(hover[0]),
                    fmt.apply(values[hover[0]][hover[1]]), Theme.series(hover[0]));
        }
        g2.dispose();
    }

    private void line(Graphics2D g2, V a, V b) {
        double[] s = cam.project(a);
        double[] t = cam.project(b);
        g2.draw(new java.awt.geom.Line2D.Double(s[0], s[1], t[0], t[1]));
    }

    /** Lightweight tooltip reusing the chart tooltip look. */
    private final class Tip extends ChartBase {
        void draw(Graphics2D g2, int x, int y, String title, String value, Color c) {
            setSize(Bar3DChart.this.getSize());
            tooltip(g2, x, y, title, List.<String[]>of(new String[]{"Value", value}), List.of(c));
        }

        @Override
        protected void paintComponent(Graphics g) {
        }
    }
}
