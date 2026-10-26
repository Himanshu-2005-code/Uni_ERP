package edu.univ.erp.ui.kit;

import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;
import edu.univ.erp.ui.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * In-window modal dialog: the window behind is frosted and dimmed, and the dialog
 * scales in with a slight overshoot. Esc closes, Enter triggers the primary action.
 */
public final class Modal {

    private final JRootPane root;
    private final Backdrop backdrop;
    private final DialogCard card;
    private final JPanel body;
    private final JPanel footer;
    private final List<Runnable> onClose = new ArrayList<>();
    private Btn primary;
    private boolean closed;
    private int width = 480;

    public Modal(Component owner, String title, String subtitle, String icon) {
        root = SwingUtilities.getRootPane(owner);
        backdrop = new Backdrop(root);
        card = new DialogCard();
        card.setLayout(new MigLayout("insets 24 26 22 26, fillx, wrap 1", "[grow, fill]", "[]14[grow, fill]18[]"));

        JPanel head = new JPanel(new MigLayout("insets 0, fillx", "[]12[grow]push[]", "[]0[]"));
        head.setOpaque(false);
        if (icon != null) {
            JLabel ic = new JLabel(new IconBubble(icon, 40, Theme.p().accent()));
            head.add(ic, "spany 2, aligny top");
        }
        JLabel t = new JLabel(title);
        t.setFont(Theme.semibold(17f));
        head.add(t, "cell 1 0, growx");
        Btn x = Btn.icon("x", "Close (Esc)");
        x.addActionListener(e -> close());
        head.add(x, "cell 2 0, spany 2, aligny top, w 34!, h 34!");
        if (subtitle != null) {
            JLabel s = new JLabel("<html>" + subtitle + "</html>");
            s.setFont(Theme.font(12.5f));
            s.setForeground(Theme.p().muted());
            head.add(s, "cell 1 1, growx, wmin 0");
        }
        card.add(head);

        body = new JPanel(new MigLayout("insets 0, fill", "[grow, fill]", "[grow, fill]"));
        body.setOpaque(false);
        card.add(body, "grow");

        footer = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        card.add(footer, "growx");

        backdrop.setLayout(null);
        backdrop.add(card);
        backdrop.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                place();
            }
        });
        bindKeys();
    }

    public Modal width(int w) {
        this.width = w;
        return this;
    }

    public Modal content(JComponent c) {
        body.removeAll();
        body.add(c, "grow");
        return this;
    }

    /** Adds a footer button. The last primary button added becomes the Enter action. */
    public Modal action(Btn b) {
        footerAdd(b);
        if (primary == null && (b.kind() == Btn.Kind.PRIMARY || b.kind() == Btn.Kind.DANGER)) primary = b;
        return this;
    }

    public Modal cancel(String text) {
        Btn b = Btn.secondary(text, null);
        b.addActionListener(e -> close());
        return action(b);
    }

    public Modal primary(Btn b) {
        primary = b;
        footerAdd(b);
        return this;
    }

    private void footerAdd(Btn b) {
        if (footer.getComponentCount() > 0) footer.add(javax.swing.Box.createHorizontalStrut(10));
        footer.add(b);
    }

    public Modal onClose(Runnable r) {
        onClose.add(r);
        return this;
    }

    private void bindKeys() {
        card.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "close");
        card.getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        card.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"), "primary");
        card.getActionMap().put("primary", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Component f = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (f instanceof javax.swing.JTextArea) return;
                if (primary != null && primary.isEnabled()) primary.doClick();
            }
        });
    }

    private void place() {
        int w = Math.min(width, backdrop.getWidth() - 40);
        card.setSize(w, 10);
        int h = Math.min(card.getPreferredSize().height, backdrop.getHeight() - 40);
        card.setBounds((backdrop.getWidth() - w) / 2, Math.max(20, (backdrop.getHeight() - h) / 2 - 10), w, h);
        card.revalidate();
    }

    public void show() {
        JLayeredPane lp = root.getLayeredPane();
        backdrop.capture();
        backdrop.setBounds(0, 0, lp.getWidth(), lp.getHeight());
        lp.add(backdrop, JLayeredPane.MODAL_LAYER);
        place();
        lp.revalidate();
        Animator.animate(380, Easing.OUT_BACK, t -> {
            card.scale = 0.92 + 0.08 * t;
            card.alpha = (float) Math.min(1, t * 1.4);
            card.repaint();
        }, null);
        Animator.animate(300, Easing.OUT_CUBIC, t -> {
            backdrop.alpha = (float) t;
            backdrop.repaint();
        }, null);
        SwingUtilities.invokeLater(this::focusFirst);
    }

    private void focusFirst() {
        Component c = firstText(body);
        if (c != null) c.requestFocusInWindow();
        else if (primary != null) primary.requestFocusInWindow();
    }

    private static Component firstText(Container c) {
        for (Component ch : c.getComponents()) {
            if (ch instanceof JTextComponent t && t.isEditable() && t.isShowing()) return ch;
            if (ch instanceof Container cc) {
                Component f = firstText(cc);
                if (f != null) return f;
            }
        }
        return null;
    }

    public void close() {
        if (closed) return;
        closed = true;
        Animator.animate(200, Easing.IN_OUT_CUBIC, t -> {
            card.scale = 1 - 0.05 * t;
            card.alpha = (float) (1 - t);
            backdrop.alpha = (float) (1 - t);
            backdrop.repaint();
        }, () -> {
            JLayeredPane lp = root.getLayeredPane();
            lp.remove(backdrop);
            lp.repaint();
            onClose.forEach(Runnable::run);
        });
    }

    /** A yes/no confirmation. */
    public static void confirm(Component owner, String title, String message, String confirmText, boolean danger,
                               Runnable onConfirm) {
        Modal m = new Modal(owner, title, null, danger ? "alert" : "info");
        JLabel msg = new JLabel("<html><div style='line-height:1.4'>" + message + "</div></html>");
        msg.setFont(Theme.font(13.5f));
        msg.setForeground(Theme.p().muted());
        m.content(msg).width(440);
        m.cancel("Cancel");
        Btn ok = danger ? Btn.danger(confirmText, null) : Btn.primary(confirmText, null);
        ok.addActionListener(e -> {
            m.close();
            onConfirm.run();
        });
        m.primary(ok);
        m.show();
    }

    // ---- painting ---------------------------------------------------------------------------

    private static final class Backdrop extends JComponent {
        private final JRootPane root;
        private Image frosted;
        private float alpha;

        Backdrop(JRootPane root) {
            this.root = root;
            addMouseListener(new MouseAdapter() { });
            addMouseMotionListener(new MouseAdapter() { });
            addMouseWheelListener(e -> { });
        }

        /** Takes a blurred snapshot of the window by down- then up-sampling. */
        void capture() {
            Container cp = root.getContentPane();
            int w = Math.max(1, cp.getWidth());
            int h = Math.max(1, cp.getHeight());
            BufferedImage full = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = full.createGraphics();
            cp.paint(g);
            g.dispose();
            Image small = full;
            int sw = w;
            int sh = h;
            for (int i = 0; i < 3; i++) {
                sw = Math.max(1, sw / 2);
                sh = Math.max(1, sh / 2);
                BufferedImage next = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = next.createGraphics();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(small, 0, 0, sw, sh, null);
                g2.dispose();
                small = next;
            }
            frosted = small;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.hq(g);
            g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0, Math.min(1, alpha))));
            if (frosted != null) {
                g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                Container cp = root.getContentPane();
                java.awt.Point o = SwingUtilities.convertPoint(cp, 0, 0, this);
                g2.drawImage(frosted, o.x, o.y, cp.getWidth(), cp.getHeight(), null);
            }
            g2.setColor(Theme.isDark() ? new Color(2, 6, 18, 150) : new Color(15, 23, 42, 95));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    private static final class DialogCard extends JPanel {
        private double scale = 0.92;
        private float alpha;

        DialogCard() {
            setOpaque(false);
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0, Math.min(1, alpha))));
            double cx = getWidth() / 2.0;
            double cy = getHeight() / 2.0;
            g2.translate(cx, cy);
            g2.scale(scale, scale);
            g2.translate(-cx, -cy);
            super.paint(g2);
            g2.dispose();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.hq(g);
            Card.paintSurface(g2, 4, 4, getWidth() - 8, getHeight() - 10, 20, 1.6, null);
            g2.dispose();
        }
    }
}
