package edu.univ.erp.ui.page.student;

import edu.univ.erp.domain.Enrollment;
import edu.univ.erp.service.SettingsService;
import edu.univ.erp.service.StudentService;
import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.anim.Easing;
import edu.univ.erp.ui.kit.Avatar;
import edu.univ.erp.ui.kit.Card;
import edu.univ.erp.ui.kit.EmptyState;
import edu.univ.erp.ui.kit.Modal;
import edu.univ.erp.ui.kit.StatCard;
import edu.univ.erp.ui.kit.UI;
import edu.univ.erp.ui.page.Page;
import edu.univ.erp.ui.page.Parts;
import edu.univ.erp.ui.page.Shell;
import edu.univ.erp.ui.theme.Theme;
import edu.univ.erp.util.TimeSlot;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** The student's week as a visual calendar. */
public class TimetablePage extends Page {

    public TimetablePage(Shell shell) {
        super(shell);
    }

    @Override
    public String title() {
        return "Timetable";
    }

    @Override
    public String subtitle() {
        return "Your week, " + SettingsService.get().term();
    }

    @Override
    public void reload() {
        load(StudentService::currentRegistrations, this::render);
    }

    private void render(List<Enrollment> regs) {
        JPanel col = column();
        int minutes = 0;
        int days = 0;
        boolean[] busy = new boolean[7];
        for (Enrollment e : regs) {
            TimeSlot t = TimeSlot.parse(e.section().dayTime());
            if (t == null) continue;
            minutes += (t.endMinute() - t.startMinute()) * t.days().size();
            for (DayOfWeek d : t.days()) busy[d.getValue() - 1] = true;
        }
        for (boolean b : busy) if (b) days++;
        JPanel stats = new JPanel(new MigLayout("insets 0, fillx, gap 18", "[grow, fill, sg s]".repeat(3), ""));
        stats.setOpaque(false);
        stats.add(new StatCard("clock", Theme.series(0), "Hours in class per week", minutes / 60.0,
                v -> String.format("%.1f", v), null));
        stats.add(new StatCard("calendar", Theme.series(1), "Days on campus", days, v -> String.valueOf(Math.round(v)), null));
        stats.add(new StatCard("layers", Theme.series(2), "Courses", regs.size(), v -> String.valueOf(Math.round(v)), null));
        col.add(stats);
        if (regs.isEmpty()) {
            col.add(Parts.section("This week", null, new EmptyState("calendar", "Your week is empty",
                    "Register for courses and they'll appear here.")));
        } else {
            Card c = new Card(new MigLayout("insets 0, fill", "[grow, fill]", "[grow, fill]"));
            c.pad(16, 16, 16, 16);
            c.add(new WeekView(regs, this), "h 620!");
            col.add(c);
        }
        setBody(col, true);
    }

    /** Calendar grid with animated class blocks and a current-time marker. */
    static final class WeekView extends JComponent {
        private record Block(Enrollment e, int day, int start, int end, Color color) {
        }

        private final List<Block> blocks = new ArrayList<>();
        private final JComponent owner;
        private final int fromHour;
        private final int toHour;
        private final int dayCount;
        private double progress;
        private int hover = -1;
        private final Runnable tick = this::repaint;

        WeekView(List<Enrollment> regs, JComponent owner) {
            this.owner = owner;
            int lo = 9 * 60;
            int hi = 17 * 60;
            boolean weekend = false;
            for (Enrollment e : regs) {
                TimeSlot t = TimeSlot.parse(e.section().dayTime());
                if (t == null) continue;
                for (DayOfWeek d : t.days()) {
                    blocks.add(new Block(e, d.getValue() - 1, t.startMinute(), t.endMinute(),
                            Avatar.colorFor(e.section().courseCode())));
                    if (d.getValue() >= 6) weekend = true;
                }
                lo = Math.min(lo, t.startMinute());
                hi = Math.max(hi, t.endMinute());
            }
            fromHour = lo / 60;
            toHour = (hi + 59) / 60;
            dayCount = weekend ? 6 : 5;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addHierarchyListener(ev -> {
                if ((ev.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                    if (isShowing()) {
                        Animator.addLoop(tick);
                        if (progress == 0) Animator.animate(1100, Easing.LINEAR, t -> progress = t, null);
                    } else {
                        Animator.removeLoop(tick);
                    }
                }
            });
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    hover = blockAt(e.getPoint());
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = -1;
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    int i = blockAt(e.getPoint());
                    if (i >= 0) details(blocks.get(i).e());
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        private static final int LEFT = 62;
        private static final int HEAD = 54;

        private double colW() {
            return (getWidth() - LEFT) / (double) dayCount;
        }

        private double yOf(int minute) {
            double span = (toHour - fromHour) * 60.0;
            return HEAD + (getHeight() - HEAD - 6) * (minute - fromHour * 60) / span;
        }

        private Rectangle2D rect(Block b) {
            double x = LEFT + b.day() * colW() + 5;
            double y = yOf(b.start()) + 2;
            return new Rectangle2D.Double(x, y, colW() - 10, yOf(b.end()) - y - 2);
        }

        private int blockAt(Point p) {
            for (int i = blocks.size() - 1; i >= 0; i--) if (rect(blocks.get(i)).contains(p)) return i;
            return -1;
        }

        private void details(Enrollment e) {
            Modal m = new Modal(owner, e.section().courseCode() + " · " + e.section().courseTitle(),
                    e.section().term(), "calendar");
            m.content(Parts.facts(new String[][]{{"Schedule", e.section().dayTime()}, {"Room", e.section().room()},
                    {"Instructor", e.section().instructorName() == null ? "TBA" : e.section().instructorName()},
                    {"Credits", String.valueOf(e.section().credits())},
                    {"Registered on", Parts.dateTime(e.enrolledAt())}})).width(440);
            m.cancel("Close");
            m.show();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.hq(g);
            Theme.Palette p = Theme.p();
            int w = getWidth();
            int h = getHeight();
            double cw = colW();
            LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
            int today = LocalDate.now().getDayOfWeek().getValue() - 1;
            // Today column tint
            if (today < dayCount) {
                g2.setColor(Theme.alpha(p.accent(), Theme.isDark() ? 0.08 : 0.045));
                g2.fill(new RoundRectangle2D.Double(LEFT + today * cw + 2, 4, cw - 4, h - 8, 16, 16));
            }
            // Day headers
            for (int d = 0; d < dayCount; d++) {
                LocalDate date = monday.plusDays(d);
                double cx = LEFT + d * cw + cw / 2;
                boolean isToday = d == today;
                g2.setFont(Theme.semibold(12f));
                g2.setColor(isToday ? p.accent() : p.muted());
                String dn = date.getDayOfWeek().toString().substring(0, 3);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(dn, (float) (cx - fm.stringWidth(dn) / 2.0), 20);
                String dd = date.format(DateTimeFormatter.ofPattern("d MMM"));
                g2.setFont(Theme.semibold(15f));
                fm = g2.getFontMetrics();
                if (isToday) {
                    g2.setColor(p.accent());
                    g2.fill(new RoundRectangle2D.Double(cx - fm.stringWidth(dd) / 2.0 - 8, 25, fm.stringWidth(dd) + 16, 22, 11, 11));
                    g2.setColor(Color.WHITE);
                } else {
                    g2.setColor(p.text());
                }
                g2.drawString(dd, (float) (cx - fm.stringWidth(dd) / 2.0), 42);
            }
            // Hour lines
            g2.setFont(Theme.font(11f));
            for (int hr = fromHour; hr <= toHour; hr++) {
                double y = yOf(hr * 60);
                g2.setColor(Theme.alpha(p.border(), 0.8));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new java.awt.geom.Line2D.Double(LEFT, y, w, y));
                g2.setColor(p.subtle());
                g2.drawString(String.format("%02d:00", hr), 8, (float) y + 4);
            }
            for (int d = 1; d < dayCount; d++) {
                g2.setColor(Theme.alpha(p.border(), 0.5));
                double x = LEFT + d * cw;
                g2.draw(new java.awt.geom.Line2D.Double(x, HEAD - 6, x, h));
            }
            // Blocks
            for (int i = 0; i < blocks.size(); i++) {
                Block b = blocks.get(i);
                double t = Math.max(0, Math.min(1, (progress * (1 + blocks.size() * 0.06) - i * 0.06) / 0.6));
                double e = Easing.OUT_BACK.apply(t);
                if (t <= 0) continue;
                Rectangle2D r = rect(b);
                boolean hot = i == hover;
                double sx = r.getCenterX();
                double sy = r.getCenterY();
                Graphics2D bg = (Graphics2D) g2.create();
                bg.setComposite(java.awt.AlphaComposite.SrcOver.derive((float) Math.min(1, t * 1.4)));
                double s = 0.85 + 0.15 * e;
                bg.translate(sx, sy);
                bg.scale(s, s);
                bg.translate(-sx, -sy);
                RoundRectangle2D rr = new RoundRectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight(), 14, 14);
                if (hot) {
                    for (int k = 5; k >= 1; k--) {
                        bg.setColor(Theme.alpha(b.color(), 0.05 * (6 - k)));
                        bg.fill(new RoundRectangle2D.Double(r.getX() - k, r.getY() - k + 3, r.getWidth() + 2 * k,
                                r.getHeight() + 2 * k, 14 + k, 14 + k));
                    }
                }
                bg.setPaint(new GradientPaint((float) r.getX(), (float) r.getY(),
                        Theme.alpha(b.color(), Theme.isDark() ? 0.42 : hot ? 0.3 : 0.2), (float) r.getX(),
                        (float) r.getMaxY(), Theme.alpha(b.color(), Theme.isDark() ? 0.28 : hot ? 0.2 : 0.12)));
                bg.fill(rr);
                bg.setColor(b.color());
                bg.fill(new RoundRectangle2D.Double(r.getX(), r.getY(), 4, r.getHeight(), 4, 4));
                bg.clip(rr);
                bg.setColor(Theme.isDark() ? Theme.brighter(b.color(), 0.45) : Theme.darker(b.color(), 0.35));
                bg.setFont(Theme.semibold(12.5f));
                bg.drawString(b.e().section().courseCode(), (float) r.getX() + 12, (float) r.getY() + 20);
                bg.setFont(Theme.font(11.5f));
                bg.setColor(Theme.isDark() ? p.text() : Theme.darker(b.color(), 0.5));
                if (r.getHeight() > 44) {
                    String title = UI.esc(b.e().section().courseTitle());
                    bg.drawString(fit(bg, title, r.getWidth() - 20), (float) r.getX() + 12, (float) r.getY() + 37);
                }
                if (r.getHeight() > 62) {
                    bg.setColor(Theme.alpha(Theme.isDark() ? p.text() : Theme.darker(b.color(), 0.5), 0.8));
                    bg.drawString(TimeSlot.format(b.start()) + "–" + TimeSlot.format(b.end()) + " · " + b.e().section().room(),
                            (float) r.getX() + 12, (float) r.getY() + 54);
                }
                bg.dispose();
            }
            // Now marker
            int now = LocalTime.now().getHour() * 60 + LocalTime.now().getMinute();
            if (today < dayCount && now >= fromHour * 60 && now <= toHour * 60) {
                double y = yOf(now);
                double x0 = LEFT + today * cw;
                g2.setColor(p.danger());
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new java.awt.geom.Line2D.Double(x0, y, x0 + cw, y));
                double pulse = 1 + 0.25 * Math.sin(System.nanoTime() / 2.5e8);
                g2.fill(new Ellipse2D.Double(x0 - 5 * pulse, y - 5 * pulse, 10 * pulse, 10 * pulse));
            }
            g2.dispose();
        }

        private static String fit(Graphics2D g, String s, double w) {
            FontMetrics fm = g.getFontMetrics();
            if (fm.stringWidth(s) <= w) return s;
            String t = s;
            while (t.length() > 1 && fm.stringWidth(t + "…") > w) t = t.substring(0, t.length() - 1);
            return t + "…";
        }
    }
}
