package edu.univ.erp.ui.three;

import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.theme.Theme;
import edu.univ.erp.ui.three.Engine3D.Camera;
import edu.univ.erp.ui.three.Engine3D.Face;
import edu.univ.erp.ui.three.Engine3D.V;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The animated backdrop of the sign-in screen: a slowly turning low-poly crystal globe,
 * rings of orbiting particles, a few satellites, drifting aurora glows and a starfield,
 * all leaning gently towards the mouse.
 */
public class LoginScene extends JComponent {

    private final List<V[]> tris = new ArrayList<>();
    private final List<double[]> stars = new ArrayList<>();
    private final Camera cam = new Camera();
    private final Runnable loop = this::repaint;
    private final long start = System.nanoTime();
    private double mx;
    private double my;
    private double tx;
    private double ty;

    public LoginScene() {
        setOpaque(true);
        buildSphere(2);
        Random r = new Random(7);
        for (int i = 0; i < 160; i++) {
            stars.add(new double[]{r.nextDouble(), r.nextDouble(), 0.2 + r.nextDouble() * 0.8, r.nextDouble() * 6.28});
        }
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) Animator.addLoop(loop);
                else Animator.removeLoop(loop);
            }
        });
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                tx = e.getX() / (double) Math.max(1, getWidth()) - 0.5;
                ty = e.getY() / (double) Math.max(1, getHeight()) - 0.5;
            }
        };
        addMouseMotionListener(m);
    }

    /** Lets the parent forward mouse movement (the form panel sits over part of the scene). */
    public void pointer(double nx, double ny) {
        tx = nx - 0.5;
        ty = ny - 0.5;
    }

    private void buildSphere(int depth) {
        double t = (1 + Math.sqrt(5)) / 2;
        V[] v = {new V(-1, t, 0), new V(1, t, 0), new V(-1, -t, 0), new V(1, -t, 0), new V(0, -1, t), new V(0, 1, t),
                new V(0, -1, -t), new V(0, 1, -t), new V(t, 0, -1), new V(t, 0, 1), new V(-t, 0, -1), new V(-t, 0, 1)};
        int[][] f = {{0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11}, {1, 5, 9}, {5, 11, 4}, {11, 10, 2},
                {10, 7, 6}, {7, 1, 8}, {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9}, {4, 9, 5}, {2, 4, 11},
                {6, 2, 10}, {8, 6, 7}, {9, 8, 1}};
        List<V[]> faces = new ArrayList<>();
        for (int[] tri : f) faces.add(new V[]{v[tri[0]].unit(), v[tri[1]].unit(), v[tri[2]].unit()});
        for (int d = 0; d < depth; d++) {
            List<V[]> next = new ArrayList<>();
            Map<String, V> mids = new HashMap<>();
            for (V[] tri : faces) {
                V a = mid(tri[0], tri[1], mids);
                V b = mid(tri[1], tri[2], mids);
                V c = mid(tri[2], tri[0], mids);
                next.add(new V[]{tri[0], a, c});
                next.add(new V[]{tri[1], b, a});
                next.add(new V[]{tri[2], c, b});
                next.add(new V[]{a, b, c});
            }
            faces = next;
        }
        // Gentle low-poly relief so the facets catch the light differently
        Random r = new Random(3);
        Map<String, V> bumped = new HashMap<>();
        for (V[] tri : faces) {
            V[] out = new V[3];
            for (int i = 0; i < 3; i++) {
                String k = key(tri[i]);
                out[i] = bumped.get(k);
                if (out[i] == null) {
                    out[i] = tri[i].times(1 + (r.nextDouble() - 0.5) * 0.06);
                    bumped.put(k, out[i]);
                }
            }
            tris.add(out);
        }
    }

    private static String key(V v) {
        return Math.round(v.x() * 1e5) + "," + Math.round(v.y() * 1e5) + "," + Math.round(v.z() * 1e5);
    }

    private static V mid(V a, V b, Map<String, V> cache) {
        String k = key(a) + "|" + key(b);
        String k2 = key(b) + "|" + key(a);
        V m = cache.get(k);
        if (m == null) m = cache.get(k2);
        if (m == null) {
            m = a.plus(b).times(0.5).unit();
            cache.put(k, m);
        }
        return m;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        int w = getWidth();
        int h = getHeight();
        double time = (System.nanoTime() - start) / 1e9;
        mx += (tx - mx) * 0.05;
        my += (ty - my) * 0.05;

        // Deep gradient sky
        g2.setPaint(new GradientPaint(0, 0, Theme.hex("#0B1030"), w, h, Theme.hex("#1B0F3D")));
        g2.fillRect(0, 0, w, h);
        aurora(g2, w * (0.25 + 0.06 * Math.sin(time * 0.3)) - mx * 40, h * (0.3 + 0.05 * Math.cos(time * 0.25)),
                Math.max(w, h) * 0.55, Theme.hex("#4F46E5"), 0.55);
        aurora(g2, w * (0.8 + 0.05 * Math.cos(time * 0.27)) - mx * 25, h * (0.75 + 0.04 * Math.sin(time * 0.33)),
                Math.max(w, h) * 0.5, Theme.hex("#06B6D4"), 0.35);
        aurora(g2, w * (0.6 + 0.07 * Math.sin(time * 0.21)), h * (0.15), Math.max(w, h) * 0.35, Theme.hex("#C026D3"), 0.25);

        // Stars with parallax and twinkle
        for (double[] s : stars) {
            double sx = (s[0] * w - mx * 30 * s[2] + w) % w;
            double sy = (s[1] * h - my * 30 * s[2] + h) % h;
            double tw = 0.45 + 0.55 * Math.abs(Math.sin(time * (0.6 + s[2]) + s[3]));
            double r = 0.6 + s[2] * 1.3;
            g2.setColor(new Color(255, 255, 255, (int) (tw * 180 * s[2])));
            g2.fill(new Ellipse2D.Double(sx, sy, r, r));
        }

        // Camera
        cam.yaw = time * 0.18 + mx * 0.7;
        cam.pitch = 0.32 + my * 0.45;
        cam.distance = 4.9;
        cam.target = new V(0, 0, 0);
        cam.viewport(w, h);
        double scale = Math.min(w, h) / 820.0;

        Graphics2D sg = (Graphics2D) g2.create();
        double cx = w / 2.0;
        double cy = h / 2.0;
        sg.translate(cx, cy);
        sg.scale(scale * 0.78, scale * 0.78);
        sg.translate(-cx, -cy);

        // Back half of the orbit rings first, then globe, then front half
        rings(sg, time, false);
        // Halo behind the globe
        double[] c0 = cam.project(new V(0, 0, 0));
        sg.setPaint(new RadialGradientPaint(new Point2D.Double(c0[0], c0[1]), (float) (c0[2] * 1.55),
                new float[]{0f, 0.6f, 1f}, new Color[]{new Color(129, 140, 248, 110), new Color(99, 102, 241, 35),
                new Color(99, 102, 241, 0)}, MultipleGradientPaint.CycleMethod.NO_CYCLE));
        sg.fill(new Ellipse2D.Double(c0[0] - c0[2] * 1.6, c0[1] - c0[2] * 1.6, c0[2] * 3.2, c0[2] * 3.2));

        List<Face> faces = new ArrayList<>(tris.size());
        Color top = Theme.hex("#38BDF8");
        Color bottom = Theme.hex("#6D28D9");
        for (V[] t : tris) {
            double y = (t[0].y() + t[1].y() + t[2].y()) / 3;
            Color c = Theme.mix(bottom, top, (y + 1) / 2);
            faces.add(new Face(t, c, null, 0.62f, true, new Color(190, 230, 255, 70)));
        }
        Engine3D.render(sg, faces, cam, new V(-0.6, 0.7, -0.5), 0.32);

        // Shimmering vertices on the front of the globe
        long seed = 0;
        for (V[] t : tris) {
            seed++;
            if (seed % 5 != 0) continue;
            V vv = cam.view(t[0]);
            if (vv.z() > cam.distance - 0.2) continue;
            double[] s = cam.screen(vv);
            double a = 0.5 + 0.5 * Math.sin(time * 2 + seed);
            sg.setColor(new Color(224, 242, 254, (int) (90 + a * 140)));
            sg.fill(new Ellipse2D.Double(s[0] - 1.6, s[1] - 1.6, 3.2, 3.2));
        }
        rings(sg, time, true);
        sg.dispose();

        // Soft vignette towards the form side
        g2.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 0), 0, h, new Color(5, 6, 20, 90)));
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }

    private void aurora(Graphics2D g2, double x, double y, double r, Color c, double strength) {
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) r, new float[]{0f, 1f},
                new Color[]{Theme.alpha(c, strength * 0.55), Theme.alpha(c, 0)}));
        g2.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
    }

    /** Draws orbiting particles; front=true draws only those nearer than the globe centre. */
    private void rings(Graphics2D g2, double time, boolean front) {
        double centreDepth = cam.view(new V(0, 0, 0)).z();
        double[][] spec = {{1.65, 0.42, 0.25, 64, 0}, {2.05, -0.3, 0.17, 48, 1}, {1.35, 1.2, -0.32, 36, 2}};
        for (double[] ring : spec) {
            double radius = ring[0];
            double tilt = ring[1];
            double speed = ring[2];
            int n = (int) ring[3];
            Color c = ring[4] == 0 ? Theme.hex("#A5B4FC") : ring[4] == 1 ? Theme.hex("#67E8F9") : Theme.hex("#F0ABFC");
            double[] prev = null;
            double prevDepth = 0;
            for (int i = 0; i <= n; i++) {
                double a = 2 * Math.PI * i / n + time * speed;
                V p = new V(Math.cos(a) * radius, 0, Math.sin(a) * radius);
                p = new V(p.x(), p.y() * Math.cos(tilt) - p.z() * Math.sin(tilt), p.y() * Math.sin(tilt) + p.z() * Math.cos(tilt));
                V v = cam.view(p);
                double[] s = cam.screen(v);
                boolean near = v.z() < centreDepth;
                if (prev != null && near == front && (prevDepth < centreDepth) == front) {
                    g2.setColor(Theme.alpha(c, 0.16));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.draw(new Line2D.Double(prev[0], prev[1], s[0], s[1]));
                }
                if (near == front && i % 4 == 0) {
                    double size = 2.2 + s[2] / 260;
                    g2.setColor(Theme.alpha(c, front ? 0.95 : 0.45));
                    g2.fill(new Ellipse2D.Double(s[0] - size / 2, s[1] - size / 2, size, size));
                }
                prev = s;
                prevDepth = v.z();
            }
            // A satellite cube riding each ring
            double a = time * speed * 1.7 + ring[4] * 2.1;
            V centre = new V(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            centre = new V(centre.x(), centre.y() * Math.cos(tilt) - centre.z() * Math.sin(tilt),
                    centre.y() * Math.sin(tilt) + centre.z() * Math.cos(tilt));
            boolean near = cam.view(centre).z() < centreDepth;
            if (near == front) {
                double s = 0.055;
                List<Face> box = new ArrayList<>(Engine3D.box(centre.x() - s, centre.y() - s, centre.z() - s,
                        centre.x() + s, centre.y() + s, centre.z() + s, Theme.brighter(c, 0.2), Theme.brighter(c, 0.55), null));
                Engine3D.render(g2, box, cam, new V(-0.6, 0.7, -0.5), 0.6);
            }
        }
    }
}
