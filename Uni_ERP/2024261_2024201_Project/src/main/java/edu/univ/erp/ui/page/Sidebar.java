package edu.univ.erp.ui.page;

import edu.univ.erp.domain.User;
import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;
import edu.univ.erp.ui.kit.Avatar;
import edu.univ.erp.ui.kit.Brand;
import edu.univ.erp.ui.kit.DropdownMenu;
import edu.univ.erp.ui.kit.Icons;
import edu.univ.erp.ui.theme.Prefs;
import edu.univ.erp.ui.theme.Theme;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The navigation rail. The active pill glides to the chosen item, the hover glow follows
 * the pointer, and the whole rail eases between expanded and collapsed widths.
 */
public class Sidebar extends JComponent {

    public static final int EXPANDED = 252;
    public static final int COLLAPSED = 78;
    private static final int ROW = 44;
    private static final int HEADER = 34;
    private static final int TOP = 96;

    private final List<Nav> items;
    private final User user;
    private final Consumer<Nav> onSelect;
    private final Consumer<JComponent> onUserMenu;
    private final JComponent userAnchor = new JComponent() { };
    private final List<int[]> layout = new ArrayList<>();
    private int selected = -1;
    private int hover = -1;
    private double pillY = -1;
    private double hoverY = -1;
    private double hoverAlpha;
    private double width;
    private boolean collapsed;
    private Animator.Anim pillAnim;
    private Animator.Anim hoverAnim;
    private Animator.Anim widthAnim;
    private boolean hoverUser;
    private boolean hoverToggle;
    private final java.util.Map<String, Integer> badges = new java.util.HashMap<>();

    public Sidebar(List<Nav> items, User user, Consumer<Nav> onSelect, Consumer<JComponent> onUserMenu) {
        this.items = items;
        this.user = user;
        this.onSelect = onSelect;
        this.onUserMenu = onUserMenu;
        this.collapsed = Prefs.sidebarCollapsed();
        this.width = collapsed ? COLLAPSED : EXPANDED;
        setLayout(null);
        add(userAnchor);
        computeLayout();
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                track(e.getPoint());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverUser = false;
                hoverToggle = false;
                setHover(-1);
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                Point p = e.getPoint();
                if (toggleRect().contains(p)) {
                    toggleCollapsed();
                    return;
                }
                if (userRect().contains(p)) {
                    userAnchor.setBounds(userRect());
                    onUserMenu.accept(userAnchor);
                    return;
                }
                int i = itemAt(p.y);
                if (i >= 0) onSelect.accept(items.get(i));
            }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
        setToolTipText("");
    }

    private void computeLayout() {
        layout.clear();
        int y = TOP;
        String group = null;
        for (int i = 0; i < items.size(); i++) {
            Nav n = items.get(i);
            if (!n.group().equals(group)) {
                group = n.group();
                y += i == 0 ? 0 : 10;
                layout.add(new int[]{-1, y});
                y += HEADER;
            }
            layout.add(new int[]{i, y});
            y += ROW + 4;
        }
    }

    private int yOf(int index) {
        for (int[] e : layout) if (e[0] == index) return e[1];
        return 0;
    }

    private int itemAt(int y) {
        for (int[] e : layout) if (e[0] >= 0 && y >= e[1] && y < e[1] + ROW) return e[0];
        return -1;
    }

    private Rectangle userRect() {
        return new Rectangle(12, getHeight() - 74, (int) width - 24, 60);
    }

    private Rectangle toggleRect() {
        return new Rectangle(12, getHeight() - 122, (int) width - 24, 38);
    }

    private void track(Point p) {
        hoverUser = userRect().contains(p);
        hoverToggle = toggleRect().contains(p);
        setHover(itemAt(p.y));
        repaint();
    }

    private void setHover(int i) {
        if (i == hover) return;
        hover = i;
        if (hoverAnim != null) hoverAnim.cancel();
        double fy = hoverY;
        double fa = hoverAlpha;
        double ty = i >= 0 ? yOf(i) : hoverY;
        double ta = i >= 0 ? 1 : 0;
        if (fy < 0) fy = ty;
        double sy = fy;
        hoverAnim = Animator.animate(180, Easing.OUT_CUBIC, t -> {
            hoverY = Animator.lerp(sy, ty, t);
            hoverAlpha = Animator.lerp(fa, ta, t);
            repaint();
        }, null);
    }

    public void select(String id) {
        int idx = -1;
        for (int i = 0; i < items.size(); i++) if (items.get(i).id().equals(id)) idx = i;
        if (idx < 0 || idx == selected) return;
        selected = idx;
        double target = yOf(idx);
        if (pillY < 0) {
            pillY = target;
            repaint();
            return;
        }
        if (pillAnim != null) pillAnim.cancel();
        double from = pillY;
        pillAnim = Animator.animate(420, Easing.OUT_BACK, t -> {
            pillY = Animator.lerp(from, target, t);
            repaint();
        }, null);
    }

    public void setBadge(String id, int count) {
        badges.put(id, count);
        repaint();
    }

    public void toggleCollapsed() {
        collapsed = !collapsed;
        Prefs.sidebarCollapsed(collapsed);
        if (widthAnim != null) widthAnim.cancel();
        double from = width;
        double to = collapsed ? COLLAPSED : EXPANDED;
        widthAnim = Animator.animate(360, Easing.IN_OUT_CUBIC, t -> {
            width = Animator.lerp(from, to, t);
            revalidate();
            repaint();
        }, null);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension((int) Math.round(width), 600);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        if (width > COLLAPSED + 20) return null;
        int i = itemAt(e.getY());
        return i >= 0 ? items.get(i).label() : null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        Theme.Palette p = Theme.p();
        int w = getWidth();
        int h = getHeight();
        double open = (width - COLLAPSED) / (double) (EXPANDED - COLLAPSED);
        g2.setPaint(new GradientPaint(0, 0, p.sidebarTop(), 0, h, p.sidebarBottom()));
        g2.fillRect(0, 0, w, h);
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(w * 0.2, 0), (float) Math.max(220, w * 1.2),
                new float[]{0f, 1f}, new Color[]{new Color(99, 102, 241, 70), new Color(99, 102, 241, 0)},
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g2.fillRect(0, 0, w, h);
        g2.setColor(new Color(255, 255, 255, 14));
        g2.fillRect(w - 1, 0, 1, h);

        // Brand
        Brand.paintMark(g2, 21, 26, 36);
        if (open > 0.05) {
            Graphics2D t = (Graphics2D) g2.create();
            t.setComposite(java.awt.AlphaComposite.SrcOver.derive((float) open));
            t.setFont(Theme.semibold(16.5f));
            t.setColor(Color.WHITE);
            t.drawString(Brand.NAME, 68, 44);
            t.setFont(Theme.font(11.5f));
            t.setColor(p.sidebarText());
            t.drawString(user.role().label() + " workspace", 68, 61);
            t.dispose();
        }

        // Hover glow and active pill
        int iw = w - 24;
        if (hoverAlpha > 0.01 && hover != selected) {
            g2.setColor(new Color(255, 255, 255, (int) (16 * hoverAlpha)));
            g2.fill(new RoundRectangle2D.Double(12, hoverY, iw, ROW, 14, 14));
        }
        if (selected >= 0 && pillY >= 0) {
            for (int i = 5; i >= 1; i--) {
                g2.setColor(new Color(99, 102, 241, 10 * (6 - i)));
                g2.fill(new RoundRectangle2D.Double(12 - i, pillY - i + 3, iw + 2 * i, ROW + 2 * i, 16 + i, 16 + i));
            }
            g2.setPaint(new GradientPaint(12, 0, Theme.hex("#6366F1"), 12 + iw, 0, Theme.hex("#8B5CF6")));
            g2.fill(new RoundRectangle2D.Double(12, pillY, iw, ROW, 14, 14));
            g2.setColor(new Color(255, 255, 255, 40));
            g2.draw(new RoundRectangle2D.Double(12.5, pillY + 0.5, iw - 1, ROW - 1, 14, 14));
        }

        for (int[] e : layout) {
            int y = e[1];
            if (e[0] < 0) {
                String group = items.get(nextItem(e)).group();
                if (open > 0.4) {
                    g2.setFont(Theme.semibold(10.5f));
                    g2.setColor(new Color(p.sidebarText().getRed(), p.sidebarText().getGreen(), p.sidebarText().getBlue(),
                            (int) (130 * open)));
                    g2.drawString(group.toUpperCase(), 26, y + 22);
                } else {
                    g2.setColor(new Color(255, 255, 255, 25));
                    g2.fillRect(26, y + 16, w - 52, 1);
                }
                continue;
            }
            Nav n = items.get(e[0]);
            boolean active = e[0] == selected;
            boolean hot = e[0] == hover;
            Color fg = active ? Color.WHITE : hot ? Color.WHITE : p.sidebarText();
            Icons.paint(g2, n.icon(), 28, y + (ROW - 20) / 2.0, 20, fg, 1.9f);
            if (open > 0.05) {
                Graphics2D t = (Graphics2D) g2.create();
                t.setComposite(java.awt.AlphaComposite.SrcOver.derive((float) open));
                t.setFont(active ? Theme.semibold(13.5f) : Theme.medium(13.5f));
                t.setColor(fg);
                FontMetrics fm = t.getFontMetrics();
                t.drawString(n.label(), 62, y + (ROW - fm.getHeight()) / 2 + fm.getAscent());
                t.dispose();
            }
            Integer badge = badges.get(n.id());
            if (badge != null && badge > 0) {
                String s = badge > 99 ? "99+" : String.valueOf(badge);
                g2.setFont(Theme.semibold(10.5f));
                FontMetrics fm = g2.getFontMetrics();
                int bw = Math.max(20, fm.stringWidth(s) + 10);
                double bx = open > 0.5 ? w - 24 - bw : 40;
                double by = open > 0.5 ? y + (ROW - 20) / 2.0 : y + 6;
                g2.setColor(active ? new Color(255, 255, 255, 60) : Theme.hex("#F43F5E"));
                g2.fill(new RoundRectangle2D.Double(bx, by, bw, open > 0.5 ? 20 : 16, 16, 16));
                g2.setColor(Color.WHITE);
                g2.drawString(s, (float) (bx + (bw - fm.stringWidth(s)) / 2.0), (float) (by + (open > 0.5 ? 14 : 12)));
            }
        }

        // Collapse toggle
        Rectangle tr = toggleRect();
        if (hoverToggle) {
            g2.setColor(new Color(255, 255, 255, 16));
            g2.fill(new RoundRectangle2D.Double(tr.x, tr.y, tr.width, tr.height, 12, 12));
        }
        Icons.paintRotated(g2, "chevron-left", 38, tr.y + tr.height / 2.0, 18, Math.PI * (1 - open), p.sidebarText(), 2f);
        if (open > 0.3) {
            Graphics2D t = (Graphics2D) g2.create();
            t.setComposite(java.awt.AlphaComposite.SrcOver.derive((float) open));
            t.setFont(Theme.medium(12.5f));
            t.setColor(p.sidebarText());
            t.drawString("Collapse", 62, tr.y + 24);
            t.dispose();
        }

        // User card
        Rectangle ur = userRect();
        g2.setColor(new Color(255, 255, 255, hoverUser ? 22 : 12));
        g2.fill(new RoundRectangle2D.Double(ur.x, ur.y, ur.width, ur.height, 16, 16));
        Avatar.paint(g2, user.displayName(), ur.x + 11, ur.y + 12, 36);
        g2.setColor(Theme.hex("#22C55E"));
        g2.fillOval(ur.x + 38, ur.y + 39, 10, 10);
        g2.setColor(p.sidebarBottom());
        g2.drawOval(ur.x + 38, ur.y + 39, 10, 10);
        if (open > 0.05) {
            Graphics2D t = (Graphics2D) g2.create();
            t.setComposite(java.awt.AlphaComposite.SrcOver.derive((float) open));
            t.clipRect(ur.x, ur.y, ur.width - 30, ur.height);
            t.setFont(Theme.semibold(13f));
            t.setColor(Color.WHITE);
            t.drawString(user.displayName(), ur.x + 58, ur.y + 27);
            t.setFont(Theme.font(11.5f));
            t.setColor(p.sidebarText());
            t.drawString("@" + user.username(), ur.x + 58, ur.y + 44);
            t.dispose();
            Icons.paint(g2, "chevron-up", ur.x + ur.width - 28, ur.y + 21, 16, p.sidebarText(), 2f);
        }
        g2.dispose();
    }

    private int nextItem(int[] header) {
        int idx = layout.indexOf(header);
        for (int i = idx + 1; i < layout.size(); i++) if (layout.get(i)[0] >= 0) return layout.get(i)[0];
        return 0;
    }
}
