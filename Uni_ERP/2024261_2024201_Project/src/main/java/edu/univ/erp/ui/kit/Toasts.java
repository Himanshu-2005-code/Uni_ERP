package edu.univ.erp.ui.kit;

import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;
import edu.univ.erp.ui.theme.Theme;

import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/** Slide-in notifications stacked in the top-right corner of the window. */
public final class Toasts {

    public enum Kind { SUCCESS, ERROR, INFO, WARNING }

    private static final WeakHashMap<JLayeredPane, List<Toast>> STACKS = new WeakHashMap<>();
    private static final int TOAST_W = 380;
    private static final int TOP = 84;

    private Toasts() {
    }

    public static void success(Component owner, String message) {
        show(owner, Kind.SUCCESS, message);
    }

    public static void error(Component owner, String message) {
        show(owner, Kind.ERROR, message);
    }

    public static void info(Component owner, String message) {
        show(owner, Kind.INFO, message);
    }

    public static void warning(Component owner, String message) {
        show(owner, Kind.WARNING, message);
    }

    public static void show(Component owner, Kind kind, String message) {
        JRootPane root = owner == null ? null : SwingUtilities.getRootPane(owner);
        if (root == null) {
            java.awt.Window w = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
            root = w == null ? null : SwingUtilities.getRootPane(w);
        }
        if (root == null) return;
        JLayeredPane lp = root.getLayeredPane();
        List<Toast> stack = STACKS.computeIfAbsent(lp, k -> {
            List<Toast> l = new ArrayList<>();
            k.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    layout(k, l, false);
                }
            });
            return l;
        });
        Toast t = new Toast(kind, message, lp, stack);
        lp.add(t, JLayeredPane.POPUP_LAYER);
        stack.add(0, t);
        layout(lp, stack, true);
        t.enter();
    }

    private static void layout(JLayeredPane lp, List<Toast> stack, boolean animate) {
        int y = TOP;
        for (Toast t : stack) {
            int h = t.preferredHeight();
            int x = lp.getWidth() - TOAST_W - 20;
            t.moveTo(x, y, h, animate);
            y += h + 10;
        }
    }

    private static final class Toast extends JComponent {
        private final Kind kind;
        private final String message;
        private final JLayeredPane lp;
        private final List<Toast> stack;
        private float alpha;
        private double slide = 40;
        private double progress = 1;
        private Animator.Anim moveAnim;
        private Animator.Anim life;
        private boolean closing;
        private List<String> lines;

        Toast(Kind kind, String message, JLayeredPane lp, List<Toast> stack) {
            this.kind = kind;
            this.message = message == null ? "" : message;
            this.lp = lp;
            this.stack = stack;
            setFont(Theme.medium(13f));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    close();
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (life != null) life.cancel();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    startLife(1800);
                }
            });
        }

        int preferredHeight() {
            FontMetrics fm = getFontMetrics(getFont());
            lines = wrap(message, fm, TOAST_W - 92);
            return Math.max(64, 30 + lines.size() * fm.getHeight());
        }

        void moveTo(int x, int y, int h, boolean animate) {
            if (getWidth() == 0 || !animate) {
                setBounds(x, y, TOAST_W, h);
                return;
            }
            if (moveAnim != null) moveAnim.cancel();
            int fx = getX();
            int fy = getY();
            moveAnim = Animator.animate(260, Easing.OUT_CUBIC, t -> setBounds((int) Animator.lerp(fx, x, t),
                    (int) Animator.lerp(fy, y, t), TOAST_W, h), null);
        }

        void enter() {
            Animator.animate(420, Easing.OUT_BACK, t -> {
                alpha = (float) Math.min(1, t * 1.6);
                slide = 40 * (1 - t);
                repaint();
            }, null);
            startLife(kind == Kind.ERROR ? 6500 : 3800);
        }

        void startLife(int ms) {
            if (closing) return;
            if (life != null) life.cancel();
            double from = progress;
            life = Animator.animate((long) (ms * from), Easing.LINEAR, t -> {
                progress = from * (1 - t);
                repaint();
            }, this::close);
        }

        void close() {
            if (closing) return;
            closing = true;
            if (life != null) life.cancel();
            Animator.animate(260, Easing.IN_OUT_CUBIC, t -> {
                alpha = (float) (1 - t);
                slide = 30 * t;
                repaint();
            }, () -> {
                lp.remove(this);
                stack.remove(this);
                lp.repaint();
                Toasts.layout(lp, stack, true);
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.hq(g);
            g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0, Math.min(1, alpha))));
            g2.translate(slide, 0);
            Theme.Palette p = Theme.p();
            Color c = switch (kind) {
                case SUCCESS -> p.success();
                case ERROR -> p.danger();
                case WARNING -> p.warning();
                default -> p.accent();
            };
            int w = getWidth() - 4;
            int h = getHeight() - 8;
            Card.paintSurface(g2, 2, 2, w, h, 14, 0.6, Theme.isDark() ? Theme.p().surfaceAlt() : null);
            g2.setColor(Theme.alpha(c, 0.14));
            g2.fill(new Ellipse2D.Double(16, h / 2.0 - 16 + 2, 32, 32));
            String icon = switch (kind) {
                case SUCCESS -> "check";
                case ERROR -> "x";
                case WARNING -> "alert";
                default -> "info";
            };
            Icons.paint(g2, icon, 23, h / 2.0 - 9 + 2, 18, c, 2.2f);
            g2.setFont(getFont());
            g2.setColor(p.text());
            FontMetrics fm = g2.getFontMetrics();
            int ty = (h - lines.size() * fm.getHeight()) / 2 + fm.getAscent() + 2;
            for (String line : lines) {
                g2.drawString(line, 62, ty);
                ty += fm.getHeight();
            }
            g2.setColor(Theme.alpha(c, 0.55));
            g2.fillRoundRect(16, h - 2, (int) ((w - 32) * progress), 2, 2, 2);
            g2.dispose();
        }
    }

    static List<String> wrap(String text, FontMetrics fm, int width) {
        List<String> out = new ArrayList<>();
        for (String para : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : para.split(" ")) {
                String next = line.length() == 0 ? word : line + " " + word;
                if (fm.stringWidth(next) > width && line.length() > 0) {
                    out.add(line.toString());
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(next);
                }
            }
            out.add(line.toString());
        }
        return out;
    }
}
