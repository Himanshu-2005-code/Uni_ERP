package edu.univ.erp.ui.kit;

import edu.univ.erp.ui.theme.Theme;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Line icons drawn from a compact path syntax on a 24x24 grid, so they stay sharp at any
 * size and follow the theme colour. Commands: M L H V Q C Z (SVG-style, absolute),
 * O cx cy r (circle), E cx cy rx ry (ellipse), R x y w h r (rounded rectangle).
 */
public final class Icons {

    private static final Map<String, String> PATHS = new HashMap<>();
    private static final Map<String, List<Shape>> CACHE = new HashMap<>();

    static {
        PATHS.put("home", "M3 10.5 L12 3 L21 10.5 M5 9 V20 H19 V9 M10 20 V14 H14 V20");
        PATHS.put("grid", "R3 3 7.5 7.5 2 R13.5 3 7.5 7.5 2 R3 13.5 7.5 7.5 2 R13.5 13.5 7.5 7.5 2");
        PATHS.put("book", "M5 4.5 Q5 3 6.5 3 H19 V18 H6.5 Q5 18 5 19.5 Q5 21 6.5 21 H19 M5 4.5 V19.5 M9 7.5 H15");
        PATHS.put("layers", "M12 3 L21 8 L12 13 L3 8 Z M3 12.5 L12 17.5 L21 12.5 M3 16.5 L12 21.5 L21 16.5");
        PATHS.put("calendar", "R3 5 18 16 2.5 M3 10 H21 M8 3 V7 M16 3 V7 M7.5 14 H9 M11.25 14 H12.75 M15 14 H16.5 M7.5 17.5 H9 M11.25 17.5 H12.75");
        PATHS.put("award", "O12 9 6 M8.6 14 L7 21 L12 18.4 L17 21 L15.4 14");
        PATHS.put("bars", "M3 20 H21 M6.5 16 V10 M11 16 V5 M15.5 16 V8 M20 16 V12");
        PATHS.put("trend", "M3 17 L9 11 L13 15 L21 7 M15 7 H21 V13");
        PATHS.put("users", "O9 8 3.5 M2.5 20 Q2.5 14 9 14 Q15.5 14 15.5 20 M16 4.6 Q19 5.4 19 8 Q19 10.6 16 11.4 M18 14.4 Q21.5 15.5 21.5 20");
        PATHS.put("user", "O12 8 4 M4 21 Q4 14 12 14 Q20 14 20 21");
        PATHS.put("sliders", "M4 6 H13 M18 6 H20 M4 12 H7 M12 12 H20 M4 18 H11 M16 18 H20 O15.5 6 2.2 O9.5 12 2.2 O13.5 18 2.2");
        PATHS.put("sun", "O12 12 4 M12 2 V4 M12 20 V22 M2 12 H4 M20 12 H22 M4.9 4.9 L6.3 6.3 M17.7 17.7 L19.1 19.1 M4.9 19.1 L6.3 17.7 M17.7 6.3 L19.1 4.9");
        PATHS.put("moon", "M20 14.6 C18.9 18.4 15.4 21 11.5 21 C6.8 21 3 17.2 3 12.5 C3 8.6 5.6 5.1 9.4 4 C8.9 5.2 8.6 6.5 8.6 7.9 C8.6 12.9 12.6 16.9 17.6 16.9 C18.5 16.9 19.3 16.8 20 14.6 Z");
        PATHS.put("logout", "M9.5 4 H5.5 Q4 4 4 5.5 V18.5 Q4 20 5.5 20 H9.5 M15 8 L19 12 L15 16 M19 12 H9");
        PATHS.put("search", "O11 11 7 M16.2 16.2 L21 21");
        PATHS.put("plus", "M12 5 V19 M5 12 H19");
        PATHS.put("minus", "M5 12 H19");
        PATHS.put("trash", "M4 7 H20 M9 7 V4.5 H15 V7 M6 7 L7 20 H17 L18 7 M10 11 V16 M14 11 V16");
        PATHS.put("edit", "M4 20 L4.6 16.2 L16 4.8 Q17.2 3.6 18.4 4.8 L19.2 5.6 Q20.4 6.8 19.2 8 L7.8 19.4 Z M14 6.8 L17.2 10");
        PATHS.put("download", "M12 4 V15 M7 10 L12 15 L17 10 M4 19.5 H20");
        PATHS.put("upload", "M12 15 V4 M7 9 L12 4 L17 9 M4 19.5 H20");
        PATHS.put("bell", "M6 16 V11 Q6 5 12 5 Q18 5 18 11 V16 L19.5 18 H4.5 Z M10 21 H14 M12 3 V5");
        PATHS.put("shield", "M12 3 L20 6 V12 Q20 18 12 21 Q4 18 4 12 V6 Z M8.5 12 L11 14.5 L15.5 10");
        PATHS.put("database", "E12 6 8 3 M4 6 V18 Q4 21 12 21 Q20 21 20 18 V6 M4 12 Q4 15 12 15 Q20 15 20 12");
        PATHS.put("clipboard", "R5 4 14 17 2.5 M9 4 V2.8 H15 V4 M8.5 10 H15.5 M8.5 14 H15.5 M8.5 18 H12.5");
        PATHS.put("check", "M5 12.5 L10 17.5 L19 7");
        PATHS.put("x", "M6 6 L18 18 M18 6 L6 18");
        PATHS.put("chevron-down", "M6 9 L12 15 L18 9");
        PATHS.put("chevron-up", "M6 15 L12 9 L18 15");
        PATHS.put("chevron-right", "M9 6 L15 12 L9 18");
        PATHS.put("chevron-left", "M15 6 L9 12 L15 18");
        PATHS.put("lock", "R5 10 14 11 2.5 M8 10 V7 Q8 3 12 3 Q16 3 16 7 V10 M12 14 V17");
        PATHS.put("unlock", "R5 10 14 11 2.5 M8 10 V7 Q8 3 12 3 Q15.6 3 16 6.2 M12 14 V17");
        PATHS.put("key", "O8 15 4.5 M11.2 11.8 L20 3 M16.5 6.5 L19.5 9.5 M14.2 8.8 L16.4 11");
        PATHS.put("refresh", "M20 11 Q19.2 5 12.5 4.5 Q7.2 4.4 5 9 M4 13 Q4.8 19 11.5 19.5 Q16.8 19.6 19 15 M20 4.5 V11 H13.5 M4 19.5 V13 H10.5");
        PATHS.put("filter", "M3 5 H21 L14 13 V20 L10 18 V13 Z");
        PATHS.put("megaphone", "M3 10 V14 Q3 15 4 15 H6 L15 19.5 V4.5 L6 9 H4 Q3 9 3 10 Z M18.5 9 Q20.5 12 18.5 15 M6.5 15 L8 21 H10.5 L9.5 16.5");
        PATHS.put("history", "O12 12 9 M12 7 V12 L15.5 14");
        PATHS.put("eye", "M2 12 Q7 5 12 5 Q17 5 22 12 Q17 19 12 19 Q7 19 2 12 Z O12 12 3");
        PATHS.put("file", "M6 3 H14 L19 8 V21 H6 Z M14 3 V8 H19 M9 13 H16 M9 17 H14");
        PATHS.put("cube", "M12 2.5 L20.5 7 V17 L12 21.5 L3.5 17 V7 Z M3.5 7 L12 11.5 L20.5 7 M12 11.5 V21.5");
        PATHS.put("menu", "M4 7 H20 M4 12 H20 M4 17 H20");
        PATHS.put("panel", "R3 4 18 16 2.5 M9 4 V20");
        PATHS.put("zap", "M13 2 L4 14 H11 L10 22 L20 9 H13 Z");
        PATHS.put("building", "M3 21 H21 M5 21 V9 L12 4 L19 9 V21 M9.5 21 V15 H14.5 V21 M9 11 H10 M14 11 H15");
        PATHS.put("cap", "M2 9 L12 4 L22 9 L12 14 Z M6 11 V16 Q12 20 18 16 V11 M22 9 V15");
        PATHS.put("info", "O12 12 9 M12 11 V16 M12 7.6 V8");
        PATHS.put("alert", "M12 3.5 L21.5 20 H2.5 Z M12 9.5 V14 M12 17 V17.3");
        PATHS.put("mail", "R3 5 18 14 2.5 M3.5 7 L12 13 L20.5 7");
        PATHS.put("sparkle", "M12 3 L13.8 10.2 L21 12 L13.8 13.8 L12 21 L10.2 13.8 L3 12 L10.2 10.2 Z");
        PATHS.put("seat", "M6 11 V7 Q6 5 8 5 H16 Q18 5 18 7 V11 M3 11.5 Q3 10 4.5 10 Q6 10 6 11.5 V14 H18 V11.5 Q18 10 19.5 10 Q21 10 21 11.5 V17 H3 Z M6 17 V20 M18 17 V20");
        PATHS.put("clock", "O12 12 9 M12 7 V12 H16");
        PATHS.put("pin", "M12 21 Q5 14 5 9.5 Q5 3 12 3 Q19 3 19 9.5 Q19 14 12 21 Z O12 9.5 2.5");
        PATHS.put("percent", "M5 19 L19 5 O7 7 2.5 O17 17 2.5");
        PATHS.put("command", "M9 9 V6 Q9 3 6 3 Q3 3 3 6 Q3 9 6 9 H18 Q21 9 21 6 Q21 3 18 3 Q15 3 15 6 V18 Q15 21 18 21 Q21 21 21 18 Q21 15 18 15 H6 Q3 15 3 18 Q3 21 6 21 Q9 21 9 18 Z");
        PATHS.put("globe", "O12 12 9 M3 12 H21 M12 3 Q7.5 7.5 7.5 12 Q7.5 16.5 12 21 Q16.5 16.5 16.5 12 Q16.5 7.5 12 3");
        PATHS.put("server", "R3 4 18 7 2 R3 13 18 7 2 M7 7.5 H7.3 M7 16.5 H7.3 M11 7.5 H17 M11 16.5 H17");
        PATHS.put("dot", "O12 12 3");
        PATHS.put("arrow-right", "M5 12 H19 M13 6 L19 12 L13 18");
        PATHS.put("arrow-up", "M12 19 V5 M6 11 L12 5 L18 11");
        PATHS.put("arrow-down", "M12 5 V19 M6 13 L12 19 L18 13");
        PATHS.put("table", "R3 4 18 16 2.5 M3 9.5 H21 M3 15 H21 M9 9.5 V20");
        PATHS.put("pie", "M12 3 V12 H21 Q21 7.5 17.5 5 Q15 3 12 3 Z M9.5 5 Q4 6.5 3.5 12.5 Q4 20 12 20.5 Q18 20.5 19.5 14.5");
    }

    private Icons() {
    }

    /** An icon in a fixed colour. */
    public static Icon of(String name, int size, Color color) {
        return new LineIcon(name, size, () -> color, false);
    }

    /** An icon that takes the colour of the component it is painted on. */
    public static Icon of(String name, int size) {
        return new LineIcon(name, size, null, false);
    }

    /** An icon whose colour is looked up at paint time (follows theme changes). */
    public static Icon of(String name, int size, Supplier<Color> color) {
        return new LineIcon(name, size, color, false);
    }

    public static Icon filled(String name, int size, Supplier<Color> color) {
        return new LineIcon(name, size, color, true);
    }

    /** Paints an icon directly. */
    public static void paint(Graphics2D g, String name, double x, double y, double size, Color color, float stroke) {
        List<Shape> shapes = shapes(name);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(x, y);
        double s = size / 24.0;
        g2.scale(s, s);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(stroke / (float) s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Shape sh : shapes) g2.draw(sh);
        g2.dispose();
    }

    static List<Shape> shapes(String name) {
        return CACHE.computeIfAbsent(name, n -> parse(PATHS.getOrDefault(n, PATHS.get("dot"))));
    }

    private static List<Shape> parse(String spec) {
        List<Shape> out = new ArrayList<>();
        String[] tok = spec.replaceAll("([A-Za-z])", " $1 ").trim().split("[\\s,]+");
        Path2D.Double path = null;
        double cx = 0;
        double cy = 0;
        int i = 0;
        while (i < tok.length) {
            String cmd = tok[i++];
            switch (cmd) {
                case "M" -> {
                    if (path == null) path = new Path2D.Double();
                    cx = num(tok, i++);
                    cy = num(tok, i++);
                    path.moveTo(cx, cy);
                }
                case "L" -> {
                    cx = num(tok, i++);
                    cy = num(tok, i++);
                    path.lineTo(cx, cy);
                }
                case "H" -> {
                    cx = num(tok, i++);
                    path.lineTo(cx, cy);
                }
                case "V" -> {
                    cy = num(tok, i++);
                    path.lineTo(cx, cy);
                }
                case "Q" -> {
                    double x1 = num(tok, i++);
                    double y1 = num(tok, i++);
                    cx = num(tok, i++);
                    cy = num(tok, i++);
                    path.quadTo(x1, y1, cx, cy);
                }
                case "C" -> {
                    double x1 = num(tok, i++);
                    double y1 = num(tok, i++);
                    double x2 = num(tok, i++);
                    double y2 = num(tok, i++);
                    cx = num(tok, i++);
                    cy = num(tok, i++);
                    path.curveTo(x1, y1, x2, y2, cx, cy);
                }
                case "Z" -> path.closePath();
                case "O" -> {
                    double x = num(tok, i++);
                    double y = num(tok, i++);
                    double r = num(tok, i++);
                    out.add(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
                }
                case "E" -> {
                    double x = num(tok, i++);
                    double y = num(tok, i++);
                    double rx = num(tok, i++);
                    double ry = num(tok, i++);
                    out.add(new Ellipse2D.Double(x - rx, y - ry, rx * 2, ry * 2));
                }
                case "R" -> {
                    double x = num(tok, i++);
                    double y = num(tok, i++);
                    double w = num(tok, i++);
                    double h = num(tok, i++);
                    double r = num(tok, i++);
                    out.add(new RoundRectangle2D.Double(x, y, w, h, r * 2, r * 2));
                }
                default -> throw new IllegalArgumentException("Bad icon command " + cmd + " in " + spec);
            }
        }
        if (path != null) out.add(0, path);
        return out;
    }

    private static double num(String[] t, int i) {
        return Double.parseDouble(t[i]);
    }

    private static final class LineIcon implements Icon {
        private final String name;
        private final int size;
        private final Supplier<Color> color;
        private final boolean fill;

        LineIcon(String name, int size, Supplier<Color> color, boolean fill) {
            this.name = name;
            this.size = size;
            this.color = color;
            this.fill = fill;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = Theme.hq(g);
            Color col = color != null ? color.get() : c != null ? c.getForeground() : Theme.p().text();
            if (c != null && !c.isEnabled()) col = Theme.alpha(col, 0.4);
            double s = size / 24.0;
            g2.translate(x, y);
            g2.scale(s, s);
            g2.setColor(col);
            float stroke = (float) (Math.max(1.35, Math.min(2.0, size / 11.0)) / s);
            g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (Shape sh : shapes(name)) {
                if (fill) g2.fill(sh);
                g2.draw(sh);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    /** Rotates an icon about its centre (used for animated chevrons). */
    public static void paintRotated(Graphics2D g, String name, double cx, double cy, double size, double radians,
                                    Color color, float stroke) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.transform(AffineTransform.getRotateInstance(radians, cx, cy));
        paint(g2, name, cx - size / 2, cy - size / 2, size, color, stroke);
        g2.dispose();
    }
}
