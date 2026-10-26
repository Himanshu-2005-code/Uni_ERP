package edu.univ.erp.ui.page;

import edu.univ.erp.ui.kit.Card;
import edu.univ.erp.ui.kit.IconBubble;
import edu.univ.erp.ui.kit.Icons;
import edu.univ.erp.ui.kit.UI;
import edu.univ.erp.ui.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Building blocks shared by the pages. */
public final class Parts {

    private Parts() {
    }

    /** A card with a title row (title, optional subtitle, right-aligned actions) and a body. */
    public static Card section(String title, String subtitle, JComponent body, JComponent... actions) {
        Card c = new Card(new MigLayout("insets 0, fill, wrap 1", "[grow, fill]", "[]14[grow, fill]"));
        c.pad(18, 20, 18, 20);
        JPanel head = new JPanel(new MigLayout("insets 0, fillx", "[grow]" + "[]".repeat(actions.length), "[]0[]"));
        head.setOpaque(false);
        head.add(UI.h2(title), "cell 0 0");
        if (subtitle != null) head.add(UI.muted(subtitle), "cell 0 1");
        for (int i = 0; i < actions.length; i++) head.add(actions[i], "cell " + (i + 1) + " 0, spany 2, aligny center, gapleft 8");
        c.add(head, "growx");
        c.add(body, "grow, wmin 0");
        return c;
    }

    /** A chart in a card at a fixed body height. */
    public static Card chart(String title, String subtitle, JComponent chart, int height, JComponent... actions) {
        JPanel wrap = new JPanel(new MigLayout("insets 0, fill", "[grow, fill]", "[grow, fill]"));
        wrap.setOpaque(false);
        wrap.add(chart, "h " + height + "!, wmin 0");
        return section(title, subtitle, wrap, actions);
    }

    /** A soft coloured strip with an icon and a message. */
    public static JComponent banner(String icon, Color color, String html) {
        JPanel p = new JPanel(new MigLayout("insets 12 16 12 16, fillx", "[]12[grow]", "[center]")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Theme.hq(g);
                g2.setColor(Theme.alpha(color, Theme.isDark() ? 0.16 : 0.09));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(Theme.alpha(color, 0.35));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.add(new JLabel(Icons.of(icon, 18, () -> color)));
        JLabel l = new UI.ThemedLabel("<html>" + html + "</html>", false);
        l.setFont(Theme.font(13f));
        p.add(l, "growx, wmin 0");
        return p;
    }

    /** The gradient greeting card at the top of each dashboard. */
    public static JComponent hero(String greeting, String line, JComponent right) {
        JPanel p = new JPanel(new MigLayout("insets 26 30 26 30, fillx", "[grow][]", "[]6[]")) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Theme.hq(g);
                int w = getWidth();
                int h = getHeight();
                RoundRectangle2D r = new RoundRectangle2D.Double(0, 0, w, h, 26, 26);
                g2.setPaint(new GradientPaint(0, 0, Theme.hex("#4F46E5"), w, h, Theme.hex("#7C3AED")));
                g2.fill(r);
                g2.clip(r);
                g2.setPaint(new RadialGradientPaint(new Point2D.Double(w * 0.85, h * 0.1), w * 0.45f,
                        new float[]{0f, 1f}, new Color[]{new Color(34, 211, 238, 120), new Color(34, 211, 238, 0)},
                        MultipleGradientPaint.CycleMethod.NO_CYCLE));
                g2.fillRect(0, 0, w, h);
                g2.setColor(new Color(255, 255, 255, 22));
                g2.setStroke(new BasicStroke(1.4f));
                for (int i = 0; i < 6; i++) {
                    double s = 120 + i * 46;
                    g2.draw(new java.awt.geom.Ellipse2D.Double(w - s * 0.72, -s * 0.45, s, s));
                }
                g2.dispose();
            }
        };
        p.setOpaque(false);
        JLabel g1 = new JLabel(greeting);
        g1.setFont(Theme.semibold(24f));
        g1.setForeground(Color.WHITE);
        p.add(g1, "cell 0 0");
        JLabel g2 = new JLabel("<html>" + line + "</html>");
        g2.setFont(Theme.font(13.5f));
        g2.setForeground(new Color(224, 231, 255));
        p.add(g2, "cell 0 1, wmin 0");
        if (right != null) p.add(right, "cell 1 0, spany 2, aligny center");
        return p;
    }

    public static String greeting() {
        int h = LocalDateTime.now().getHour();
        return h < 5 ? "Working late" : h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : "Good evening";
    }

    public static String firstName(String name) {
        String n = name.replaceAll("^(Dr\\.|Prof\\.|Mr\\.|Ms\\.|Mrs\\.)\\s*", "").trim();
        return n.split("\\s+")[0];
    }

    public static Color gradeColor(String letter) {
        if (letter == null) return Theme.p().muted();
        return switch (letter) {
            case "A+", "A" -> Theme.p().success();
            case "A-", "B" -> Theme.series(0);
            case "B-", "C" -> Theme.series(6);
            case "C-", "D" -> Theme.p().warning();
            case "F" -> Theme.p().danger();
            default -> Theme.p().muted();
        };
    }

    public static Color percentColor(double pct) {
        return pct >= 80 ? Theme.p().success() : pct >= 60 ? Theme.series(0) : pct >= 40 ? Theme.p().warning()
                : Theme.p().danger();
    }

    public static Color statusColor(Object status) {
        String s = String.valueOf(status);
        return switch (s) {
            case "ACTIVE", "REGISTERED", "Registered", "Open" -> Theme.p().success();
            case "LOCKED", "DROPPED", "Full", "Dropped" -> Theme.p().danger();
            case "COMPLETED", "Completed" -> Theme.series(0);
            case "ADMIN", "Administrator" -> Theme.series(5);
            case "INSTRUCTOR", "Instructor" -> Theme.series(1);
            case "STUDENT", "Student" -> Theme.series(0);
            default -> Theme.p().muted();
        };
    }

    public static String date(LocalDate d) {
        return d == null ? "—" : d.format(DateTimeFormatter.ofPattern("d MMM yyyy"));
    }

    public static String dateTime(LocalDateTime d) {
        return d == null ? "—" : d.format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm"));
    }

    /** A compact row of label/value pairs. */
    public static JPanel facts(String[][] pairs) {
        JPanel p = new JPanel(new MigLayout("insets 0, fillx, wrap 2, gapy 10", "[]push[]", ""));
        p.setOpaque(false);
        for (String[] kv : pairs) {
            p.add(UI.muted(kv[0]));
            JLabel v = new UI.ThemedLabel(kv[1], false);
            v.setFont(Theme.semibold(13f));
            p.add(v);
        }
        return p;
    }

    public static JLabel bubble(String icon, Color c, int size) {
        return new JLabel(new IconBubble(icon, size, c));
    }

    /** Thin rounded progress bar. */
    public static JComponent bar(double ratio, Color color) {
        return new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(100, 8);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Theme.hq(g);
                int h = 6;
                int y = (getHeight() - h) / 2;
                g2.setColor(Theme.isDark() ? Theme.p().border() : Theme.hex("#E8EBF3"));
                g2.fill(new RoundRectangle2D.Double(0, y, getWidth(), h, h, h));
                g2.setColor(color);
                g2.fill(new RoundRectangle2D.Double(0, y, Math.max(h, getWidth() * Math.min(1, ratio)), h, h, h));
                g2.dispose();
            }
        };
    }
}
