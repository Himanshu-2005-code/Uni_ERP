package edu.univ.erp.ui.kit;

import com.formdev.flatlaf.FlatClientProperties;
import edu.univ.erp.ui.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

/** Small factories for consistently styled labels, fields and layouts. */
public final class UI {

    private UI() {
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.semibold(24f));
        return l;
    }

    public static JLabel h2(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.semibold(16f));
        return l;
    }

    public static JLabel h3(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.semibold(14f));
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new ThemedLabel(text, true);
        l.setFont(Theme.font(12.5f));
        return l;
    }

    public static JLabel text(String text, float size) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(size));
        return l;
    }

    /**
     * Note for HTML text: Swing scales CSS px by roughly 1.3x on screen, so a block written as
     * width:340px renders about 440 pixels wide. Widths in this code base are written that way.
     */
    public static JLabel html(String html, float size, boolean muted) {
        JLabel l = new ThemedLabel("<html>" + html + "</html>", muted);
        l.setFont(Theme.font(size));
        return l;
    }

    /** A label that re-reads its colour from the theme so it survives light/dark switches. */
    public static final class ThemedLabel extends JLabel {
        private final boolean muted;

        public ThemedLabel(String text, boolean muted) {
            super(text);
            this.muted = muted;
        }

        @Override
        public Color getForeground() {
            return muted ? Theme.p().muted() : Theme.p().text();
        }
    }

    public static JTextField field(String placeholder, String icon) {
        JTextField f = new JTextField();
        style(f, placeholder, icon);
        return f;
    }

    public static JPasswordField password(String placeholder) {
        JPasswordField f = new JPasswordField();
        style(f, placeholder, "lock");
        f.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true; showCapsLock: true");
        return f;
    }

    private static void style(JTextField f, String placeholder, String icon) {
        f.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        if (icon != null) {
            f.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Icons.of(icon, 16, () -> Theme.p().muted()));
        }
        f.putClientProperty(FlatClientProperties.STYLE, "margin: 4,10,4,10");
        f.setFont(Theme.font(13.5f));
        f.setPreferredSize(new Dimension(f.getPreferredSize().width, 40));
    }

    public static JTextField search(String placeholder) {
        JTextField f = field(placeholder, "search");
        f.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        return f;
    }

    public static JTextArea area(int rows) {
        JTextArea a = new JTextArea(rows, 20);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(Theme.font(13.5f));
        a.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        return a;
    }

    public static JScrollPane areaScroll(JTextArea a) {
        JScrollPane s = new JScrollPane(a);
        s.putClientProperty(FlatClientProperties.STYLE, "arc: 12");
        return s;
    }

    /** A labelled form field: caption above, optional hint below. */
    public static JPanel labeled(String label, Component field, String hint) {
        JPanel p = new JPanel(new MigLayout("insets 0, fillx, wrap 1, gap 0", "[grow, fill]", "[]6[]" + (hint != null ? "4[]" : "")));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(Theme.medium(12.5f));
        p.add(l);
        p.add(field, "h 40!");
        if (hint != null) {
            JLabel h = muted(hint);
            h.setFont(Theme.font(11.5f));
            p.add(h);
        }
        return p;
    }

    public static JPanel labeledArea(String label, JComponent field) {
        JPanel p = new JPanel(new MigLayout("insets 0, fill, wrap 1, gap 0", "[grow, fill]", "[]6[grow, fill]"));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(Theme.medium(12.5f));
        p.add(l);
        p.add(field, "grow");
        return p;
    }

    /** A vertically scrolling, transparent container with smooth wheel steps. */
    public static JScrollPane scroll(JComponent content) {
        JScrollPane s = new JScrollPane(new TrackWidth(content));
        s.setBorder(null);
        s.setOpaque(false);
        s.getViewport().setOpaque(false);
        s.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        s.getVerticalScrollBar().setUnitIncrement(22);
        return s;
    }

    /** Makes scrolled content follow the viewport width so rows wrap instead of overflowing. */
    static final class TrackWidth extends JPanel implements javax.swing.Scrollable {
        TrackWidth(JComponent c) {
            super(new java.awt.BorderLayout());
            setOpaque(false);
            add(c);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle r, int o, int d) {
            return 22;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle r, int o, int d) {
            return Math.max(60, r.height - 60);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() > getPreferredSize().height;
        }
    }

    public static JPanel clear(MigLayout layout) {
        JPanel p = new JPanel(layout);
        p.setOpaque(false);
        return p;
    }

    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
