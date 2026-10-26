package edu.univ.erp.ui.kit;

import com.formdev.flatlaf.FlatClientProperties;
import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;
import edu.univ.erp.ui.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Locale;

/** A yyyy-mm-dd field with a calendar that drops down and slides between months. */
public class DateField extends JPanel {

    private final JTextField text = new JTextField();
    private DropdownMenu menu;

    public DateField() {
        super(new MigLayout("insets 0, fill", "[grow, fill]", "[grow, fill]"));
        setOpaque(false);
        text.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "YYYY-MM-DD");
        text.setFont(Theme.font(13.5f));
        JButton cal = new JButton(Icons.of("calendar", 16, () -> Theme.p().muted()));
        cal.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        cal.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cal.setToolTipText("Pick a date");
        cal.addActionListener(e -> open());
        text.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, cal);
        add(text);
    }

    public LocalDate date() {
        String s = text.getText().trim();
        if (s.isEmpty()) return null;
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw new edu.univ.erp.service.ServiceException("\"" + s + "\" isn't a date. Use YYYY-MM-DD.");
        }
    }

    public void setDate(LocalDate d) {
        text.setText(d == null ? "" : d.toString());
    }

    private void open() {
        LocalDate start;
        try {
            start = date();
        } catch (RuntimeException e) {
            start = null;
        }
        Month month = new Month(start == null ? LocalDate.now() : start, d -> {
            setDate(d);
            if (menu != null) menu.close();
        });
        menu = new DropdownMenu().width(300).maxHeight(360);
        menu.header(month);
        menu.setItems(java.util.List.of());
        menu.show(this, DropdownMenu.Align.LEFT);
    }

    /** Month grid; arrows slide to the previous/next month. */
    private static final class Month extends JComponent {
        private YearMonth ym;
        private final LocalDate selected;
        private final java.util.function.Consumer<LocalDate> pick;
        private int hover = -1;
        private double slide;
        private Animator.Anim anim;

        Month(LocalDate sel, java.util.function.Consumer<LocalDate> pick) {
            this.selected = sel;
            this.ym = YearMonth.from(sel);
            this.pick = pick;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int h = cellAt(e.getPoint());
                    if (h != hover) {
                        hover = h;
                        repaint();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = -1;
                    repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    Point p = e.getPoint();
                    if (p.y < 40) {
                        if (p.x < 44) go(-1);
                        else if (p.x > getWidth() - 44) go(1);
                        return;
                    }
                    int c = cellAt(p);
                    LocalDate d = dateOf(c);
                    if (d != null) pick.accept(d);
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        private void go(int dir) {
            ym = ym.plusMonths(dir);
            if (anim != null) anim.cancel();
            anim = Animator.animate(260, Easing.OUT_CUBIC, t -> {
                slide = dir * 30 * (1 - t);
                repaint();
            }, null);
        }

        private int offset() {
            return ym.atDay(1).getDayOfWeek().getValue() - 1;
        }

        private LocalDate dateOf(int cell) {
            if (cell < 0) return null;
            int day = cell - offset() + 1;
            return day >= 1 && day <= ym.lengthOfMonth() ? ym.atDay(day) : null;
        }

        private int cellAt(Point p) {
            double cw = getWidth() / 7.0;
            int col = (int) (p.x / cw);
            int row = (p.y - 70) / 34;
            if (p.y < 70 || row > 5 || col > 6) return -1;
            return row * 7 + col;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(280, 70 + 6 * 34);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.hq(g);
            Theme.Palette p = Theme.p();
            int w = getWidth();
            g2.setFont(Theme.semibold(14f));
            FontMetrics fm = g2.getFontMetrics();
            String title = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + ym.getYear();
            g2.setColor(p.text());
            g2.drawString(title, (w - fm.stringWidth(title)) / 2, 26);
            Icons.paint(g2, "chevron-left", 12, 12, 18, p.muted(), 2f);
            Icons.paint(g2, "chevron-right", w - 30, 12, 18, p.muted(), 2f);
            double cw = w / 7.0;
            g2.setFont(Theme.semibold(10.5f));
            g2.setColor(p.subtle());
            for (int i = 0; i < 7; i++) {
                String d = DayOfWeek.of(i + 1).getDisplayName(TextStyle.SHORT, Locale.ENGLISH).substring(0, 2);
                g2.drawString(d, (float) (i * cw + (cw - g2.getFontMetrics().stringWidth(d)) / 2), 56);
            }
            g2.translate(slide, 0);
            g2.setFont(Theme.medium(12.5f));
            fm = g2.getFontMetrics();
            LocalDate today = LocalDate.now();
            for (int c = 0; c < 42; c++) {
                LocalDate d = dateOf(c);
                if (d == null) continue;
                double x = (c % 7) * cw;
                double y = 70 + (c / 7) * 34;
                boolean sel = d.equals(selected);
                if (sel || c == hover) {
                    g2.setColor(sel ? p.accent() : Theme.isDark() ? Theme.alpha(java.awt.Color.WHITE, 0.08) : p.accentSoft());
                    g2.fill(new RoundRectangle2D.Double(x + 3, y, cw - 6, 30, 10, 10));
                }
                if (d.equals(today) && !sel) {
                    g2.setColor(p.accent());
                    g2.draw(new RoundRectangle2D.Double(x + 3.5, y + 0.5, cw - 7, 29, 10, 10));
                }
                String s = String.valueOf(d.getDayOfMonth());
                g2.setColor(sel ? java.awt.Color.WHITE : d.getDayOfWeek().getValue() >= 6 ? p.muted() : p.text());
                g2.drawString(s, (float) (x + (cw - fm.stringWidth(s)) / 2), (float) (y + 20));
            }
            g2.dispose();
        }
    }
}
