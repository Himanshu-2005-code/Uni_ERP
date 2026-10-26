package edu.univ.erp.ui.three;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A tiny software 3D renderer for Java2D: perspective camera, flat Lambert shading,
 * back-face culling and painter's-algorithm depth sorting. Returns the projected
 * polygons so callers can hit-test the mouse against them.
 */
public final class Engine3D {

    private Engine3D() {
    }

    public record V(double x, double y, double z) {
        public V plus(V o) {
            return new V(x + o.x, y + o.y, z + o.z);
        }

        public V minus(V o) {
            return new V(x - o.x, y - o.y, z - o.z);
        }

        public V times(double s) {
            return new V(x * s, y * s, z * s);
        }

        public V cross(V o) {
            return new V(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
        }

        public double dot(V o) {
            return x * o.x + y * o.y + z * o.z;
        }

        public double length() {
            return Math.sqrt(dot(this));
        }

        public V unit() {
            double l = length();
            return l == 0 ? this : times(1 / l);
        }
    }

    /** An orbit camera around a target. Positive pitch looks down from above. */
    public static final class Camera {
        public double yaw = 0.6;
        public double pitch = 0.45;
        public double distance = 12;
        public double fov = Math.toRadians(40);
        public V target = new V(0, 0, 0);
        /** Screen-space framing: extra magnification and offset applied after projection. */
        public double zoom = 1;
        public double offX;
        public double offY;
        private int w;
        private int h;
        private double focal;
        private double cy;
        private double sy;
        private double cp;
        private double sp;

        public void viewport(int width, int height) {
            w = width;
            h = height;
            focal = Math.min(width, height * 1.25) / (2 * Math.tan(fov / 2));
            cy = Math.cos(yaw);
            sy = Math.sin(yaw);
            cp = Math.cos(pitch);
            sp = Math.sin(pitch);
        }

        /** World to view space (camera looks along +z). */
        public V view(V p) {
            double x = p.x - target.x;
            double y = p.y - target.y;
            double z = p.z - target.z;
            double x1 = x * cy - z * sy;
            double z1 = x * sy + z * cy;
            double y2 = y * cp + z1 * sp;
            double z2 = -y * sp + z1 * cp;
            return new V(x1, y2, z2 + distance);
        }

        public V rotate(V n) {
            double x1 = n.x * cy - n.z * sy;
            double z1 = n.x * sy + n.z * cy;
            return new V(x1, n.y * cp + z1 * sp, -n.y * sp + z1 * cp);
        }

        /** Projects a view-space point to screen {x, y, scale}. */
        public double[] screen(V v) {
            double d = Math.max(0.05, v.z);
            double s = focal * zoom / d;
            return new double[]{w / 2.0 + offX + v.x * s, h / 2.0 + offY - v.y * s, s};
        }

        public double[] project(V world) {
            return screen(view(world));
        }
    }

    /** A planar polygon with a colour and an optional tag used for picking. */
    public record Face(V[] pts, Color color, Object tag, float alpha, boolean cull, Color edge, V pivot) {
        public Face(V[] pts, Color color, Object tag) {
            this(pts, color, tag, 1f, true, null, null);
        }

        public Face(V[] pts, Color color, Object tag, float alpha, boolean cull, Color edge) {
            this(pts, color, tag, alpha, cull, edge, null);
        }
    }

    public record Drawn(Path2D shape, Object tag, double depth) {
    }

    /**
     * Shades, sorts and fills the faces. Light is a view-space direction towards the light.
     * Returns drawn polygons from back to front.
     */
    public static List<Drawn> render(Graphics2D g, List<Face> faces, Camera cam, V light, double ambient) {
        record Item(Face f, V[] view, double depth, double group, double shade) {
        }
        List<Item> items = new ArrayList<>(faces.size());
        V l = light.unit();
        for (Face f : faces) {
            V[] vv = new V[f.pts().length];
            double depth = 0;
            for (int i = 0; i < vv.length; i++) {
                vv[i] = cam.view(f.pts()[i]);
                depth += vv[i].z;
            }
            depth /= vv.length;
            V n = vv[1].minus(vv[0]).cross(vv[2].minus(vv[0])).unit();
            // Camera sits at the origin of view space looking along +z: visible faces point towards -z
            V toCam = vv[0].times(-1).unit();
            double facing = n.dot(toCam);
            if (f.cull() && facing <= 0) continue;
            if (facing < 0) n = n.times(-1);
            double lambert = Math.max(0, n.dot(l));
            double group = f.pivot() == null ? depth : cam.view(f.pivot()).z;
            items.add(new Item(f, vv, depth, group, ambient + (1 - ambient) * lambert));
        }
        // Whole objects back to front (correct for separate boxes on a floor), then faces within each
        items.sort(Comparator.comparingDouble(Item::group).thenComparingDouble(Item::depth).reversed());
        List<Drawn> out = new ArrayList<>(items.size());
        for (Item it : items) {
            Path2D poly = new Path2D.Double();
            for (int i = 0; i < it.view.length; i++) {
                double[] s = cam.screen(it.view[i]);
                if (i == 0) poly.moveTo(s[0], s[1]);
                else poly.lineTo(s[0], s[1]);
            }
            poly.closePath();
            Color c = shade(it.f.color(), it.shade, it.f.alpha());
            g.setColor(c);
            g.fill(poly);
            if (it.f.edge() != null) {
                g.setColor(it.f.edge());
                g.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(poly);
            } else {
                g.setColor(c);
                g.setStroke(new BasicStroke(0.6f));
                g.draw(poly); // seals hairline gaps between neighbouring faces
            }
            out.add(new Drawn(poly, it.f.tag(), it.depth));
        }
        return out;
    }

    public static Color shade(Color c, double k, float alpha) {
        k = Math.max(0, Math.min(1.25, k));
        int r = (int) Math.min(255, c.getRed() * k);
        int g = (int) Math.min(255, c.getGreen() * k);
        int b = (int) Math.min(255, c.getBlue() * k);
        return new Color(r, g, b, Math.round(alpha * c.getAlpha()));
    }

    /** The five visible faces of an axis-aligned box (no bottom). */
    public static List<Face> box(double x0, double y0, double z0, double x1, double y1, double z1, Color side, Color top,
                                 Object tag) {
        V a = new V(x0, y0, z0);
        V b = new V(x1, y0, z0);
        V c = new V(x1, y0, z1);
        V d = new V(x0, y0, z1);
        V e = new V(x0, y1, z0);
        V f = new V(x1, y1, z0);
        V gg = new V(x1, y1, z1);
        V h = new V(x0, y1, z1);
        V pivot = new V((x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2);
        return List.of(
                new Face(new V[]{e, h, gg, f}, top, tag, 1f, true, null, pivot),
                new Face(new V[]{a, e, f, b}, side, tag, 1f, true, null, pivot),
                new Face(new V[]{b, f, gg, c}, side, tag, 1f, true, null, pivot),
                new Face(new V[]{c, gg, h, d}, side, tag, 1f, true, null, pivot),
                new Face(new V[]{d, h, e, a}, side, tag, 1f, true, null, pivot));
    }
}
