package edu.univ.erp.ui.screen;

import com.formdev.flatlaf.FlatClientProperties;
import edu.univ.erp.ui.kit.Icons;
import edu.univ.erp.ui.theme.Theme;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.geom.RoundRectangle2D;

/** Frosted-glass card and dark-styled inputs for the screens that sit on the 3D scene. */
final class Glass {

    static final Color TEXT = new Color(0xF1F5FF);
    static final Color MUTED = new Color(0xA9B1D6);
    static final String FIELD_STYLE = "background: #121839; foreground: #EEF2FF; borderColor: #2D3766; "
            + "focusedBorderColor: #818CF8; placeholderForeground: #7680AC; caretColor: #FFFFFF; arc: 14; "
            + "margin: 4,10,4,10; focusWidth: 2; focusColor: #6366F166";

    private Glass() {
    }

    static JTextField field(String placeholder, String icon) {
        JTextField f = new JTextField();
        f.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        f.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Icons.of(icon, 16, MUTED));
        f.putClientProperty(FlatClientProperties.STYLE, FIELD_STYLE);
        f.setFont(Theme.font(14f));
        f.setPreferredSize(new Dimension(10, 46));
        f.setMinimumSize(new Dimension(10, 46));
        return f;
    }

    static JPasswordField password(String placeholder) {
        JPasswordField f = new JPasswordField();
        f.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        f.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Icons.of("lock", 16, MUTED));
        f.putClientProperty(FlatClientProperties.STYLE, FIELD_STYLE + "; showRevealButton: true; showCapsLock: true");
        f.setFont(Theme.font(14f));
        f.setPreferredSize(new Dimension(10, 46));
        f.setMinimumSize(new Dimension(10, 46));
        return f;
    }

    static JLabel label(String text, float size, boolean bold, Color c) {
        JLabel l = new JLabel(text);
        l.setFont(bold ? Theme.semibold(size) : Theme.font(size));
        l.setForeground(c);
        return l;
    }

    /** A translucent panel with a lit edge, used for the sign-in card. */
    static class Card extends JPanel {
        float alpha = 1f;
        double offsetY;
        double offsetX;

        Card(LayoutManager l) {
            super(l);
            setOpaque(false);
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0, Math.min(1, alpha))));
            g2.translate(offsetX, offsetY);
            super.paint(g2);
            g2.dispose();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.hq(g);
            int w = getWidth();
            int h = getHeight();
            for (int i = 10; i >= 1; i--) {
                g2.setColor(new Color(2, 4, 20, 10));
                g2.fill(new RoundRectangle2D.Double(-i, -i + 6, w + 2 * i, h + 2 * i, 28 + i, 28 + i));
            }
            RoundRectangle2D r = new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 28, 28);
            g2.setPaint(new GradientPaint(0, 0, new Color(24, 30, 70, 215), 0, h, new Color(12, 15, 40, 225)));
            g2.fill(r);
            g2.setPaint(new GradientPaint(0, 0, new Color(255, 255, 255, 60), w, h, new Color(255, 255, 255, 10)));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(r);
            g2.dispose();
        }
    }
}
