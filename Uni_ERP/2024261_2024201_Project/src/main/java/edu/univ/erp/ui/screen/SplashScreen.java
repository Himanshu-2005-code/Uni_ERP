package edu.univ.erp.ui.screen;

import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.kit.Brand;
import edu.univ.erp.ui.theme.Theme;
import edu.univ.erp.ui.three.LoginScene;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;

/** Brand and a spinner over the 3D scene while the app connects. */
class SplashScreen extends JLayeredPane {

    private final String message;
    private final long start = System.nanoTime();

    SplashScreen(String message) {
        this.message = message;
        LoginScene scene = new LoginScene();
        add(scene, Integer.valueOf(0));
        JComponent overlay = new JComponent() {
            private final Runnable tick = this::repaint;

            @Override
            public void addNotify() {
                super.addNotify();
                Animator.addLoop(tick);
            }

            @Override
            public void removeNotify() {
                Animator.removeLoop(tick);
                super.removeNotify();
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Theme.hq(g);
                double t = (System.nanoTime() - start) / 1e9;
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                Brand.paintMark(g2, cx - 36, cy - 120, 72);
                g2.setFont(Theme.semibold(30f));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(Color.WHITE);
                g2.drawString(Brand.NAME, cx - fm.stringWidth(Brand.NAME) / 2, cy - 10);
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(255, 255, 255, 40));
                g2.drawOval(cx - 14, cy + 30, 28, 28);
                g2.setColor(Theme.hex("#A5B4FC"));
                g2.draw(new Arc2D.Double(cx - 14, cy + 30, 28, 28, -t * 360, 100 + 60 * Math.sin(t * 3), Arc2D.OPEN));
                g2.setFont(Theme.font(13f));
                fm = g2.getFontMetrics();
                g2.setColor(Glass.MUTED);
                g2.drawString(message, cx - fm.stringWidth(message) / 2, cy + 88);
                g2.dispose();
            }
        };
        add(overlay, Integer.valueOf(1));
    }

    @Override
    public void doLayout() {
        for (java.awt.Component c : getComponents()) c.setBounds(0, 0, getWidth(), getHeight());
    }
}
