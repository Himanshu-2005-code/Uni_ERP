package edu.univ.erp.ui.kit;

import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;
import edu.univ.erp.ui.theme.Theme;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A floating menu that unfolds from its anchor. The hover highlight glides between
 * rows instead of jumping. Closes on outside click, Esc, or choosing an item.
 */
public class DropdownMenu extends JPanel {

    public static final class Item {
        final String icon;
        final String text;
        final String sub;
        final Runnable action;
        final boolean danger;
        final boolean separator;
        final boolean header;
        final boolean checked;

        private Item(String icon, String text, String sub, Runnable action, boolean danger, boolean separator,
                     boolean header, boolean checked) {
            this.icon = icon;
            this.text = text;
            this.sub = sub;
            this.action = action;
            this.danger = danger;
            this.separator = separator;
            this.header = header;
            this.checked = checked;
        }

        public static Item of(String icon, String text, Runnable action) {
            return new Item(icon, text, null, action, false, false, false, false);
        }

        public static Item rich(String icon, String text, String sub, Runnable action) {
            return new Item(icon, text, sub, action, false, false, false, false);
        }

        public static Item danger(String icon, String text, Runnable action) {
            return new Item(icon, text, null, action, true, false, false, false);
        }

        public static Item check(String text, boolean checked, Runnable action) {
            return new Item(null, text, null, action, false, false, false, checked);
        }

        public static Item header(String text) {
            return new Item(null, text, null, null, false, false, true, false);
        }

        public static Item separator() {
            return new Item(null, null, null, null, false, true, false, false);
        }

        boolean selectable() {
            return !separator && !header && action != null;
        }
    }

    public enum Align { LEFT, RIGHT }

    private static DropdownMenu open;

    private final List<Item> items = new ArrayList<>();
    private final Rows rows = new Rows();
    private final JScrollPane scroll;
    private JComponent header;
    private JRootPane root;
    private Component anchor;
    private double reveal;
    private float alpha;
    private boolean closing;
    private AWTEventListener outside;
    private Runnable onClose;
    private int menuWidth = 240;
    private int maxHeight = 340;

    public DropdownMenu() {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 10, 8));
        scroll = new JScrollPane(rows);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "esc");
        getActionMap().put("esc", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        bindArrow("DOWN", 1);
        bindArrow("UP", -1);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "enter");
        getActionMap().put("enter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rows.activate(rows.hoverIndex);
            }
        });
    }

    private void bindArrow(String key, int dir) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), "arrow" + dir);
        getActionMap().put("arrow" + dir, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rows.step(dir);
            }
        });
    }

    public DropdownMenu width(int w) {
        menuWidth = w;
        return this;
    }

    public DropdownMenu maxHeight(int h) {
        maxHeight = h;
        return this;
    }

    public DropdownMenu header(JComponent c) {
        if (header != null) remove(header);
        header = c;
        add(c, BorderLayout.NORTH);
        return this;
    }

    public DropdownMenu onClose(Runnable r) {
        onClose = r;
        return this;
    }

    public DropdownMenu add(Item item) {
        items.add(item);
        return this;
    }

    public void setItems(List<Item> list) {
        items.clear();
        items.addAll(list);
        rows.hoverIndex = -1;
        rows.highlightY = -1;
        rows.revalidate();
        rows.repaint();
        if (root != null) place();
    }

    public static boolean isOpen() {
        return open != null;
    }

    public void show(Component anchor, Align align) {
        if (open != null) open.closeNow();
        this.anchor = anchor;
        root = SwingUtilities.getRootPane(anchor);
        if (root == null) return;
        JLayeredPane lp = root.getLayeredPane();
        lp.add(this, JLayeredPane.POPUP_LAYER);
        this.align = align;
        place();
        open = this;
        closing = false;
        Animator.animate(320, Easing.OUT_EXPO, t -> {
            reveal = t;
            alpha = (float) Math.min(1, t * 1.8);
            repaint();
        }, null);
        outside = ev -> {
            if (ev.getID() == MouseEvent.MOUSE_PRESSED && ev.getSource() instanceof Component c) {
                if (!SwingUtilities.isDescendingFrom(c, this) && !SwingUtilities.isDescendingFrom(c, anchor)) close();
            } else if (ev.getID() == WindowEvent.WINDOW_DEACTIVATED) {
                close();
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(outside, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.WINDOW_EVENT_MASK);
        if (header == null) requestFocusInWindow();
    }

    private Align align = Align.LEFT;

    private void place() {
        JLayeredPane lp = root.getLayeredPane();
        Point p = SwingUtilities.convertPoint(anchor, 0, anchor.getHeight() + 6, lp);
        int w = Math.max(menuWidth, 0);
        int contentH = rows.getPreferredSize().height + (header == null ? 0 : header.getPreferredSize().height)
                + getInsets().top + getInsets().bottom + 2;
        int h = Math.min(contentH, maxHeight);
        int x = align == Align.RIGHT ? p.x + anchor.getWidth() - w : p.x;
        x = Math.max(8, Math.min(x, lp.getWidth() - w - 8));
        int y = p.y;
        if (y + h > lp.getHeight() - 8) {
            Point up = SwingUtilities.convertPoint(anchor, 0, -6, lp);
            if (up.y - h > 8) y = up.y - h;
            else h = lp.getHeight() - 8 - y;
        }
        setBounds(x, y, w, h);
        revalidate();
    }

    public void close() {
        if (closing) return;
        closing = true;
        Animator.animate(160, Easing.IN_OUT_CUBIC, t -> {
            reveal = 1 - t * 0.35;
            alpha = (float) (1 - t);
            repaint();
        }, this::closeNow);
    }

    private void closeNow() {
        if (outside != null) Toolkit.getDefaultToolkit().removeAWTEventListener(outside);
        outside = null;
        if (getParent() != null) {
            java.awt.Container parent = getParent();
            parent.remove(this);
            parent.repaint();
        }
        if (open == this) open = null;
        if (onClose != null) onClose.run();
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, Math.min(1f, alpha))));
        int h = (int) Math.ceil(getHeight() * (0.25 + 0.75 * reveal));
        g2.translate(0, -8 * (1 - reveal));
        g2.clip(new Rectangle(0, 0, getWidth(), h + 8));
        super.paint(g2);
        g2.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        Card.paintSurface(g2, 3, 2, getWidth() - 6, getHeight() - 7, 14, 1.4,
                Theme.isDark() ? Theme.p().surfaceAlt() : null);
        g2.dispose();
    }

    /** The painted item rows, with a gliding hover highlight. */
    private final class Rows extends JComponent implements Scrollable {
        private static final int ROW = 38;
        private static final int ROW_RICH = 52;
        private static final int SEP = 11;
        private static final int HEAD = 30;
        private int hoverIndex = -1;
        private double highlightY = -1;
        private double highlightH;
        private Animator.Anim glide;

        Rows() {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    hoverTo(indexAt(e.getY()));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hoverTo(-1);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    activate(indexAt(e.getY()));
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        int heightOf(Item it) {
            return it.separator ? SEP : it.header ? HEAD : it.sub != null ? ROW_RICH : ROW;
        }

        int yOf(int index) {
            int y = 0;
            for (int i = 0; i < index; i++) y += heightOf(items.get(i));
            return y;
        }

        int indexAt(int y) {
            int acc = 0;
            for (int i = 0; i < items.size(); i++) {
                int h = heightOf(items.get(i));
                if (y >= acc && y < acc + h) return items.get(i).selectable() ? i : -1;
                acc += h;
            }
            return -1;
        }

        void hoverTo(int index) {
            if (index == hoverIndex) return;
            hoverIndex = index;
            if (glide != null) glide.cancel();
            if (index < 0) {
                repaint();
                return;
            }
            double ty = yOf(index);
            double th = heightOf(items.get(index));
            if (highlightY < 0) {
                highlightY = ty;
                highlightH = th;
                repaint();
                return;
            }
            double fy = highlightY;
            double fh = highlightH;
            glide = Animator.animate(170, Easing.OUT_CUBIC, t -> {
                highlightY = Animator.lerp(fy, ty, t);
                highlightH = Animator.lerp(fh, th, t);
                repaint();
            }, null);
            scrollRectToVisible(new Rectangle(0, (int) ty, 10, (int) th));
        }

        void step(int dir) {
            if (items.isEmpty()) return;
            int i = hoverIndex;
            for (int n = 0; n < items.size(); n++) {
                i = Math.floorMod(i + dir, items.size());
                if (items.get(i).selectable()) {
                    hoverTo(i);
                    return;
                }
            }
        }

        void activate(int index) {
            if (index < 0 || index >= items.size() || !items.get(index).selectable()) return;
            Item it = items.get(index);
            close();
            SwingUtilities.invokeLater(it.action);
        }

        @Override
        public Dimension getPreferredSize() {
            int h = 0;
            for (Item it : items) h += heightOf(it);
            return new Dimension(menuWidth - 16, Math.max(h, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.hq(g);
            Theme.Palette p = Theme.p();
            int w = getWidth();
            if (hoverIndex >= 0 && highlightY >= 0) {
                Item hi = items.get(hoverIndex);
                g2.setColor(hi.danger ? Theme.alpha(p.danger(), 0.1)
                        : Theme.isDark() ? Theme.alpha(Color.WHITE, 0.07) : p.accentSoft());
                Shape r = new RoundRectangle2D.Double(0, highlightY + 1, w, highlightH - 2, 10, 10);
                g2.fill(r);
            }
            int y = 0;
            for (int i = 0; i < items.size(); i++) {
                Item it = items.get(i);
                int h = heightOf(it);
                if (it.separator) {
                    g2.setColor(p.border());
                    g2.fillRect(6, y + h / 2, w - 12, 1);
                } else if (it.header) {
                    g2.setFont(Theme.semibold(10.5f));
                    g2.setColor(p.subtle());
                    g2.drawString(it.text.toUpperCase(), 10, y + 20);
                } else {
                    boolean hover = i == hoverIndex;
                    Color fg = it.danger ? p.danger() : it.action == null ? p.subtle() : hover ? p.text()
                            : Theme.mix(p.text(), p.muted(), 0.2);
                    int x = 12;
                    if (it.icon != null) {
                        Icons.paint(g2, it.icon, x, y + (h - 17) / 2.0, 17, it.danger ? p.danger()
                                : hover ? p.accent() : p.muted(), 1.8f);
                        x += 28;
                    }
                    g2.setFont(it.checked ? Theme.semibold(13f) : Theme.medium(13f));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.setColor(fg);
                    int textRight = w - (it.checked ? 36 : 12);
                    if (it.sub != null) {
                        g2.drawString(ellipsize(it.text, fm, textRight - x), x, y + 22);
                        g2.setFont(Theme.font(11.5f));
                        g2.setColor(p.muted());
                        g2.drawString(ellipsize(it.sub, g2.getFontMetrics(), textRight - x), x, y + 39);
                    } else {
                        g2.drawString(ellipsize(it.text, fm, textRight - x), x, y + (h - fm.getHeight()) / 2 + fm.getAscent());
                    }
                    if (it.checked) Icons.paint(g2, "check", w - 30, y + (h - 16) / 2.0, 16, p.accent(), 2.2f);
                }
                y += h;
            }
            g2.dispose();
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
            return ROW;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return ROW * 5;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    static String ellipsize(String s, FontMetrics fm, int width) {
        if (s == null) return "";
        if (fm.stringWidth(s) <= width) return s;
        String t = s;
        while (t.length() > 1 && fm.stringWidth(t + "…") > width) t = t.substring(0, t.length() - 1);
        return t + "…";
    }
}
