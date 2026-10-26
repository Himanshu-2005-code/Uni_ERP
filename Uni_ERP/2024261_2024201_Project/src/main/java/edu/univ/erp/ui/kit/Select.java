package edu.univ.erp.ui.kit;

import com.formdev.flatlaf.FlatClientProperties;
import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;
import edu.univ.erp.ui.theme.Theme;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A dropdown field. The chevron turns as the menu unfolds; long lists get a filter box.
 */
public class Select<T> extends JComponent {

    private final List<T> options = new ArrayList<>();
    private final List<Consumer<T>> listeners = new ArrayList<>();
    private Function<T, String> label = String::valueOf;
    private Function<T, String> icon = t -> null;
    private T selected;
    private String placeholder = "Select…";
    private String leadingIcon;
    private double turn;
    private double hover;
    private boolean open;
    private Animator.Anim turnAnim;
    private Animator.Anim hoverAnim;
    private int menuWidth = -1;
    private DropdownMenu menu;

    public Select() {
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(Theme.medium(13f));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!isEnabled()) return;
                requestFocusInWindow();
                if (open) {
                    if (menu != null) menu.close();
                    return;
                }
                openMenu();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                animateHover(1);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                animateHover(0);
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!open && (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_DOWN
                        || e.getKeyCode() == KeyEvent.VK_ENTER)) {
                    openMenu();
                    e.consume();
                }
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    public Select<T> labels(Function<T, String> f) {
        label = f;
        repaint();
        return this;
    }

    public Select<T> icons(Function<T, String> f) {
        icon = f;
        return this;
    }

    public Select<T> placeholder(String p) {
        placeholder = p;
        repaint();
        return this;
    }

    public Select<T> leading(String iconName) {
        leadingIcon = iconName;
        repaint();
        return this;
    }

    public Select<T> menuWidth(int w) {
        menuWidth = w;
        return this;
    }

    public Select<T> options(List<T> list) {
        options.clear();
        options.addAll(list);
        if (selected != null && !options.contains(selected)) selected = null;
        repaint();
        return this;
    }

    public List<T> options() {
        return List.copyOf(options);
    }

    public Select<T> onChange(Consumer<T> l) {
        listeners.add(l);
        return this;
    }

    public T selected() {
        return selected;
    }

    public void select(T value) {
        boolean changed = !Objects.equals(value, selected);
        selected = value;
        repaint();
        if (changed) listeners.forEach(l -> l.accept(value));
    }

    /** Selects without notifying listeners. */
    public void setSelectedQuietly(T value) {
        selected = value;
        repaint();
    }

    private void animateHover(double target) {
        if (hoverAnim != null) hoverAnim.cancel();
        double from = hover;
        hoverAnim = Animator.animate(160, Easing.OUT_CUBIC, t -> {
            hover = Animator.lerp(from, target, t);
            repaint();
        }, null);
    }

    private void turnTo(double target) {
        if (turnAnim != null) turnAnim.cancel();
        double from = turn;
        turnAnim = Animator.animate(300, Easing.OUT_BACK, t -> {
            turn = Animator.lerp(from, target, t);
            repaint();
        }, null);
    }

    private void openMenu() {
        open = true;
        turnTo(1);
        DropdownMenu menu = new DropdownMenu().width(menuWidth > 0 ? menuWidth : Math.max(getWidth(), 200));
        this.menu = menu;
        menu.onClose(() -> {
            open = false;
            turnTo(0);
            requestFocusInWindow();
        });
        if (options.size() > 8) {
            JTextField filter = new JTextField();
            filter.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Type to filter…");
            filter.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Icons.of("search", 15,
                    () -> Theme.p().muted()));
            filter.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createEmptyBorder(2, 2, 8, 2), filter.getBorder()));
            menu.header(filter);
            filter.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    menu.setItems(items(menu, filter.getText()));
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    menu.setItems(items(menu, filter.getText()));
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                }
            });
            javax.swing.SwingUtilities.invokeLater(filter::requestFocusInWindow);
        }
        menu.setItems(items(menu, ""));
        menu.show(this, DropdownMenu.Align.LEFT);
    }

    private List<DropdownMenu.Item> items(DropdownMenu menu, String filter) {
        List<DropdownMenu.Item> list = new ArrayList<>();
        String f = filter.toLowerCase().trim();
        for (T o : options) {
            String text = label.apply(o);
            if (!f.isEmpty() && !text.toLowerCase().contains(f)) continue;
            boolean isSel = Objects.equals(o, selected);
            String ic = icon.apply(o);
            DropdownMenu.Item it = ic != null ? DropdownMenu.Item.of(ic, text, () -> select(o))
                    : DropdownMenu.Item.check(text, isSel, () -> select(o));
            list.add(it);
        }
        if (list.isEmpty()) list.add(DropdownMenu.Item.header("No matches"));
        return list;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int w = 60;
        for (T o : options) w = Math.max(w, fm.stringWidth(label.apply(o)));
        return new Dimension(Math.min(w + 64 + (leadingIcon != null ? 26 : 0), 360), 40);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.hq(g);
        Theme.Palette p = Theme.p();
        int w = getWidth();
        int h = getHeight();
        RoundRectangle2D r = new RoundRectangle2D.Double(1, 1, w - 2, h - 2, 12, 12);
        g2.setColor(Theme.mix(Theme.isDark() ? p.surfaceAlt() : p.surface(), p.hover(), hover * 0.6));
        g2.fill(r);
        boolean focus = isFocusOwner() || open;
        if (focus) {
            g2.setColor(Theme.alpha(p.accent(), 0.25));
            g2.setStroke(new BasicStroke(3f));
            g2.draw(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 13, 13));
        }
        g2.setColor(focus ? p.accent() : Theme.mix(p.border(), p.muted(), hover * 0.3));
        g2.setStroke(new BasicStroke(1.1f));
        g2.draw(r);
        int x = 14;
        if (leadingIcon != null) {
            Icons.paint(g2, leadingIcon, x, (h - 16) / 2.0, 16, p.muted(), 1.8f);
            x += 26;
        }
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        String text = selected == null ? placeholder : label.apply(selected);
        g2.setColor(!isEnabled() ? p.subtle() : selected == null ? p.subtle() : p.text());
        g2.drawString(DropdownMenu.ellipsize(text, fm, w - x - 40), x, (h - fm.getHeight()) / 2 + fm.getAscent());
        Icons.paintRotated(g2, "chevron-down", w - 22, h / 2.0, 16, Math.PI * turn, focus ? p.accent() : p.muted(), 2f);
        g2.dispose();
    }
}
