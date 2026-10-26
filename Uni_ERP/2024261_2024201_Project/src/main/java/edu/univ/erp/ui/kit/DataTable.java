package edu.univ.erp.ui.kit;

import edu.univ.erp.ui.anim.Animator;
import edu.univ.erp.ui.theme.Theme;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A typed table over a list of records. Rows fade in when data arrives, the hovered row
 * is highlighted, columns sort on click, and {@link #filter(String)} narrows rows live.
 */
public class DataTable<T> extends JPanel {

    private record Col<T>(String name, int width, Function<T, Object> value, TableCellRenderer renderer,
                          int align) {
    }

    private final List<Col<T>> cols = new ArrayList<>();
    private final List<T> rows = new ArrayList<>();
    private final Model model = new Model();
    private final Table table = new Table();
    private final TableRowSorter<Model> sorter = new TableRowSorter<>(model);
    private final JScrollPane scroll;
    private EmptyState empty;
    private Function<T, String> searchText = Object::toString;
    private Function<T, List<DropdownMenu.Item>> contextMenu;
    private int hoverRow = -1;
    private long appear = 0;
    private final Runnable appearTick = this::appearFrame;

    public DataTable() {
        super(new BorderLayout());
        setOpaque(false);
        table.setModel(model);
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setFont(Theme.font(13f));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(Theme.semibold(11.5f));
        table.setShowVerticalLines(false);
        MouseAdapter m = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r != hoverRow) {
                    int old = hoverRow;
                    hoverRow = r;
                    repaintRow(old);
                    repaintRow(r);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                int old = hoverRow;
                hoverRow = -1;
                repaintRow(old);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeMenu(e);
            }
        };
        table.addMouseListener(m);
        table.addMouseMotionListener(m);
        scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.getViewport().setBackground(Theme.p().surface());
        add(scroll, BorderLayout.CENTER);
    }

    private void repaintRow(int r) {
        if (r < 0) return;
        Rectangle rect = table.getCellRect(r, 0, true);
        table.repaint(0, rect.y, table.getWidth(), rect.height);
    }

    private void maybeMenu(MouseEvent e) {
        if (!e.isPopupTrigger() || contextMenu == null) return;
        int r = table.rowAtPoint(e.getPoint());
        if (r < 0) return;
        table.setRowSelectionInterval(r, r);
        T item = rows.get(table.convertRowIndexToModel(r));
        DropdownMenu menu = new DropdownMenu().width(210);
        menu.setItems(contextMenu.apply(item));
        JComponent anchor = new JComponent() { };
        anchor.setBounds(e.getX(), e.getY() - 4, 1, 1);
        table.add(anchor);
        menu.onClose(() -> table.remove(anchor));
        menu.show(anchor, DropdownMenu.Align.LEFT);
    }

    // ---- column builders --------------------------------------------------------------------

    public DataTable<T> col(String name, int width, Function<T, Object> value) {
        cols.add(new Col<>(name, width, value, null, SwingConstants.LEADING));
        return this;
    }

    public DataTable<T> num(String name, int width, Function<T, Object> value) {
        cols.add(new Col<>(name, width, value, null, SwingConstants.TRAILING));
        return this;
    }

    public DataTable<T> strong(String name, int width, Function<T, Object> value) {
        cols.add(new Col<>(name, width, value, new StrongRenderer(), SwingConstants.LEADING));
        return this;
    }

    /** A coloured pill; colour chosen from the cell value. */
    public DataTable<T> badge(String name, int width, Function<T, Object> value, Function<Object, Color> color) {
        cols.add(new Col<>(name, width, value, new BadgeRenderer(color), SwingConstants.LEADING));
        return this;
    }

    /** A progress bar from 0..1 with a caption; value must be a double[] {ratio} or Number. */
    public DataTable<T> progress(String name, int width, Function<T, Double> ratio, Function<T, String> caption) {
        cols.add(new Col<>(name, width, t -> new Object[]{ratio.apply(t), caption.apply(t)}, new ProgressRenderer(),
                SwingConstants.LEADING));
        return this;
    }

    /** An avatar with a name and a secondary line. */
    public DataTable<T> person(String name, int width, Function<T, String> title, Function<T, String> sub) {
        cols.add(new Col<>(name, width, t -> new String[]{title.apply(t), sub.apply(t)}, new PersonRenderer(),
                SwingConstants.LEADING));
        return this;
    }

    public DataTable<T> search(Function<T, String> f) {
        searchText = f;
        return this;
    }

    public DataTable<T> menu(Function<T, List<DropdownMenu.Item>> f) {
        contextMenu = f;
        return this;
    }

    public DataTable<T> empty(String icon, String title, String text) {
        empty = new EmptyState(icon, title, text);
        return this;
    }

    public DataTable<T> rowHeight(int h) {
        table.setRowHeight(h);
        return this;
    }

    public DataTable<T> onDoubleClick(Consumer<T> action) {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    T t = selected();
                    if (t != null) action.accept(t);
                }
            }
        });
        return this;
    }

    public DataTable<T> onSelect(Consumer<T> action) {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) action.accept(selected());
        });
        return this;
    }

    // ---- data -------------------------------------------------------------------------------

    public void setRows(List<T> list) {
        rows.clear();
        rows.addAll(list);
        model.fireTableStructureChanged();
        configureColumns();
        updateEmpty();
        appear = System.nanoTime();
        Animator.addLoop(appearTick);
    }

    public List<T> rows() {
        return List.copyOf(rows);
    }

    public T selected() {
        int r = table.getSelectedRow();
        return r < 0 ? null : rows.get(table.convertRowIndexToModel(r));
    }

    public void select(T item) {
        int i = rows.indexOf(item);
        if (i < 0) return;
        int v = table.convertRowIndexToView(i);
        if (v >= 0) table.setRowSelectionInterval(v, v);
    }

    public int visibleCount() {
        return table.getRowCount();
    }

    public void filter(String text) {
        String q = text == null ? "" : text.trim().toLowerCase();
        if (q.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends Model, ? extends Integer> entry) {
                    String s = searchText.apply(rows.get(entry.getIdentifier()));
                    return s != null && s.toLowerCase().contains(q);
                }
            });
        }
        updateEmpty();
    }

    public void filter(java.util.function.Predicate<T> p) {
        sorter.setRowFilter(p == null ? null : new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends Model, ? extends Integer> entry) {
                return p.test(rows.get(entry.getIdentifier()));
            }
        });
        updateEmpty();
    }

    public JTable table() {
        return table;
    }

    private void updateEmpty() {
        if (empty == null) return;
        boolean none = table.getRowCount() == 0;
        if (none && empty.getParent() == null) {
            remove(scroll);
            add(empty, BorderLayout.CENTER);
        } else if (!none && empty.getParent() != null) {
            remove(empty);
            add(scroll, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
    }

    /** Table header cell aligned like its column, with a hairline underneath. */
    public static final class HeaderRenderer extends DefaultTableCellRenderer {
        private final java.util.function.IntUnaryOperator align;

        public HeaderRenderer(java.util.function.IntUnaryOperator align) {
            this.align = align;
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, false, false, r, c);
            Theme.Palette p = Theme.p();
            setHorizontalAlignment(align.applyAsInt(t.convertColumnIndexToModel(c)));
            setFont(Theme.semibold(11f));
            setForeground(p.muted());
            setBackground(p.surfaceAlt());
            setOpaque(true);
            setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createMatteBorder(0, 0, 1, 0, p.border()),
                    javax.swing.BorderFactory.createEmptyBorder(0, 14, 0, 14)));
            return this;
        }
    }

    private void configureColumns() {
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer(i -> i < cols.size() ? cols.get(i).align()
                : SwingConstants.LEADING));
        for (int i = 0; i < cols.size(); i++) {
            Col<T> c = cols.get(i);
            TableColumn tc = table.getColumnModel().getColumn(i);
            if (c.width() > 0) {
                tc.setPreferredWidth(c.width());
                tc.setMinWidth(Math.min(c.width(), 60));
            }
            if (c.renderer() != null) {
                tc.setCellRenderer(c.renderer());
            } else {
                PlainRenderer r = new PlainRenderer();
                r.setHorizontalAlignment(c.align());
                tc.setCellRenderer(r);
            }
            int idx = i;
            sorter.setComparator(i, (Comparator<Object>) (a, b) -> compare(a, b, idx));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compare(Object a, Object b, int col) {
        if (a instanceof Object[] x && b instanceof Object[] y) return compare(x[0], y[0], col);
        if (a instanceof String[] x && b instanceof String[] y) return x[0].compareToIgnoreCase(y[0]);
        if (a == null) return b == null ? 0 : -1;
        if (b == null) return 1;
        if (a instanceof Number x && b instanceof Number y) return Double.compare(x.doubleValue(), y.doubleValue());
        if (a instanceof Comparable ca && a.getClass() == b.getClass()) return ca.compareTo(b);
        return String.valueOf(a).compareToIgnoreCase(String.valueOf(b));
    }

    private void appearFrame() {
        double elapsed = (System.nanoTime() - appear) / 1e6;
        table.repaint();
        if (elapsed > 260 + Math.min(table.getRowCount(), 18) * 28) Animator.removeLoop(appearTick);
    }

    /** 0..1 fade-in progress for a view row. */
    private double appearOf(int viewRow) {
        double elapsed = (System.nanoTime() - appear) / 1e6 - Math.min(viewRow, 18) * 28;
        return Math.max(0, Math.min(1, elapsed / 260));
    }

    private final class Model extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return cols.size();
        }

        @Override
        public String getColumnName(int c) {
            return cols.get(c).name().toUpperCase();
        }

        @Override
        public Object getValueAt(int r, int c) {
            return cols.get(c).value().apply(rows.get(r));
        }
    }

    private final class Table extends JTable {
        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component c = super.prepareRenderer(renderer, row, column);
            boolean sel = isRowSelected(row);
            Theme.Palette p = Theme.p();
            Color bg = sel ? p.accentSoft() : row == hoverRow ? p.hover() : p.surface();
            c.setBackground(bg);
            if (!sel && c instanceof JComponent jc && !(renderer instanceof StrongRenderer)) jc.setForeground(p.text());
            return c;
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            // Row entrance: cover not-yet-visible rows with the surface colour, fading out
            Graphics2D g2 = (Graphics2D) g.create();
            Rectangle clip = g2.getClipBounds();
            int first = clip == null ? 0 : Math.max(0, rowAtPoint(clip.getLocation()));
            int last = Math.min(getRowCount() - 1, first + 40);
            for (int r = first; r <= last; r++) {
                double a = appearOf(r);
                if (a >= 1) continue;
                Rectangle rect = getCellRect(r, 0, true);
                g2.setColor(Theme.alpha(Theme.p().surface(), 1 - a));
                g2.fillRect(0, rect.y, getWidth(), rect.height);
            }
            g2.dispose();
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            int r = rowAtPoint(e.getPoint());
            int c = columnAtPoint(e.getPoint());
            if (r < 0 || c < 0) return null;
            Object v = getValueAt(r, c);
            if (v instanceof String s && s.length() > 28) return s;
            return null;
        }
    }

    // ---- renderers ----------------------------------------------------------------------------

    static class PlainRenderer extends DefaultTableCellRenderer {
        PlainRenderer() {
            setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 14, 0, 14));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v == null ? "—" : v, s, false, r, c);
            setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 14, 0, 14));
            setFont(Theme.font(13f));
            return this;
        }
    }

    static final class StrongRenderer extends PlainRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            super.getTableCellRendererComponent(t, v, s, f, r, c);
            setFont(Theme.semibold(13f));
            setForeground(Theme.p().text());
            return this;
        }
    }

    static final class BadgeRenderer extends JComponent implements TableCellRenderer {
        private final Function<Object, Color> color;
        private Object value;

        BadgeRenderer(Function<Object, Color> color) {
            this.color = color;
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            value = v;
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.hq(g);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            if (value != null && !String.valueOf(value).isEmpty()) {
                Badge.paint(g2, String.valueOf(value), color.apply(value), 12, (getHeight() - 22) / 2, true,
                        Theme.semibold(11.5f));
            } else {
                g2.setColor(Theme.p().subtle());
                g2.setFont(Theme.font(13f));
                g2.drawString("—", 14, getHeight() / 2 + 5);
            }
            g2.dispose();
        }
    }

    static final class ProgressRenderer extends JComponent implements TableCellRenderer {
        private double ratio;
        private String caption;

        ProgressRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            Object[] a = (Object[]) v;
            ratio = a[0] == null ? 0 : (Double) a[0];
            caption = (String) a[1];
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.hq(g);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            Theme.Palette p = Theme.p();
            int x = 14;
            int barW = Math.max(40, getWidth() - 100);
            int y = getHeight() / 2 - 3;
            g2.setColor(Theme.isDark() ? p.border() : Theme.hex("#E8EBF3"));
            g2.fill(new RoundRectangle2D.Double(x, y, barW, 6, 6, 6));
            Color c = ratio >= 1 ? p.danger() : ratio >= 0.85 ? p.warning() : p.accent();
            g2.setColor(c);
            g2.fill(new RoundRectangle2D.Double(x, y, Math.max(6, barW * Math.min(1, ratio)), 6, 6, 6));
            g2.setFont(Theme.medium(12f));
            g2.setColor(p.muted());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(caption, x + barW + 10, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    static final class PersonRenderer extends JComponent implements TableCellRenderer {
        private String title;
        private String sub;

        PersonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            String[] a = (String[]) v;
            title = a[0];
            sub = a[1];
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.hq(g);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            int s = 30;
            Avatar.paint(g2, title, 12, (getHeight() - s) / 2, s);
            Theme.Palette p = Theme.p();
            int tx = 12 + s + 10;
            int avail = getWidth() - tx - 10;
            g2.setFont(Theme.semibold(13f));
            g2.setColor(p.text());
            FontMetrics fm = g2.getFontMetrics();
            int midY = getHeight() / 2;
            if (sub == null || sub.isBlank()) {
                g2.drawString(DropdownMenu.ellipsize(title, fm, avail), tx, midY + fm.getAscent() / 2 - 1);
            } else {
                g2.drawString(DropdownMenu.ellipsize(title, fm, avail), tx, midY - 2);
                g2.setFont(Theme.font(11.5f));
                g2.setColor(p.muted());
                g2.drawString(DropdownMenu.ellipsize(sub, g2.getFontMetrics(), avail), tx, midY + 13);
            }
            g2.dispose();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width, Math.max(d.height, 200));
    }
}
