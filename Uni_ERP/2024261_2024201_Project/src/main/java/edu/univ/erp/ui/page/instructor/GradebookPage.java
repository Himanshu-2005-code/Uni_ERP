package edu.univ.erp.ui.page.instructor;

import edu.univ.erp.domain.GradeComponent;
import edu.univ.erp.domain.GradeEntry;
import edu.univ.erp.domain.RosterEntry;
import edu.univ.erp.domain.Section;
import edu.univ.erp.service.InstructorService;
import edu.univ.erp.ui.kit.Async;
import edu.univ.erp.ui.kit.Avatar;
import edu.univ.erp.ui.kit.Badge;
import edu.univ.erp.ui.kit.Btn;
import edu.univ.erp.ui.kit.Card;
import edu.univ.erp.ui.kit.EmptyState;
import edu.univ.erp.ui.kit.Modal;
import edu.univ.erp.ui.kit.Select;
import edu.univ.erp.ui.kit.Toasts;
import edu.univ.erp.ui.kit.UI;
import edu.univ.erp.ui.page.Page;
import edu.univ.erp.ui.page.Parts;
import edu.univ.erp.ui.page.Shell;
import edu.univ.erp.ui.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Spreadsheet-style marks entry for one section. */
public class GradebookPage extends Page {

    private List<Section> sections = List.of();
    private InstructorService.Gradebook gb;
    private Model model;
    private JTable table;
    private final JLabel dirtyLabel = UI.text("", 12.5f);
    private final Btn save = Btn.primary("Save changes", "check");
    private final Btn discard = Btn.ghost("Discard", "x");

    public GradebookPage(Shell shell) {
        super(shell);
        save.addActionListener(e -> saveChanges());
        discard.addActionListener(e -> reload());
    }

    @Override
    public String title() {
        return "Gradebook";
    }

    @Override
    public String subtitle() {
        return gb == null ? "Enter and publish marks" : gb.section().courseCode() + " · " + gb.section().courseTitle()
                + " · " + gb.section().term();
    }

    @Override
    public void reload() {
        load(() -> {
            List<Section> s = InstructorService.sections();
            Integer id = Selection.sectionId;
            List<Section> ordered = Selection.order(s);
            if (id == null && !ordered.isEmpty()) id = ordered.get(0).id();
            return new Object[]{s, id == null ? null : InstructorService.gradebook(id)};
        }, r -> {
            @SuppressWarnings("unchecked")
            List<Section> s = (List<Section>) r[0];
            sections = s;
            gb = (InstructorService.Gradebook) r[1];
            if (gb != null) Selection.sectionId = gb.section().id();
            shell.refreshTitle(this);
            render();
        });
    }

    private void render() {
        JPanel col = column();
        if (sections.isEmpty() || gb == null) {
            col.add(new EmptyState("clipboard", "No sections yet", "Sections you teach will appear here."));
            setBody(col, true);
            return;
        }
        Select<Section> picker = Selection.picker(sections);
        picker.onChange(sec -> {
            if (model != null && model.dirtyCount() > 0) {
                Toasts.warning(this, "Unsaved marks were discarded.");
            }
            reload();
        });
        Btn scheme = Btn.secondary("Assessment scheme", "sliders");
        scheme.addActionListener(e -> editScheme());
        Btn export = Btn.secondary("Export CSV", "download");
        export.addActionListener(e -> exportCsv());
        Btn publish = Btn.primary("Publish final grades", "award");
        publish.addActionListener(e -> publish());
        JPanel bar = new JPanel(new MigLayout("insets 0, fillx", "[grow, fill, 220:380:420]push[][][]", "[center]"));
        bar.setOpaque(false);
        bar.add(picker, "h 40!");
        bar.add(scheme);
        bar.add(export);
        bar.add(publish);
        col.add(bar);

        StringBuilder sb = new StringBuilder();
        for (GradeComponent c : gb.components()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append("<b>").append(UI.esc(c.name())).append("</b> /").append(fmt(c.maxScore())).append(" (")
                    .append(fmt(c.weight())).append("%)");
        }
        col.add(Parts.banner("info", Theme.p().accent(), gb.roster().size() + " students · " + sb
                + " · Type marks straight into the table; blank means not assessed yet."));

        model = new Model();
        table = new JTable(model);
        table.setRowHeight(44);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setCellSelectionEnabled(true);
        table.putClientProperty("terminateEditOnFocusLost", true);
        table.setSurrendersFocusOnKeystroke(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFont(Theme.font(13f));
        table.getTableHeader().setFont(Theme.semibold(11.5f));
        JTextField editor = new JTextField();
        editor.setHorizontalAlignment(SwingConstants.RIGHT);
        editor.setFont(Theme.semibold(13f));
        editor.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.p().accent(), 2),
                BorderFactory.createEmptyBorder(0, 8, 0, 8)));
        DefaultCellEditor ed = new DefaultCellEditor(editor);
        ed.setClickCountToStart(1);
        table.getTableHeader().setDefaultRenderer(new edu.univ.erp.ui.kit.DataTable.HeaderRenderer(
                i -> i <= 1 ? SwingConstants.LEADING : i == model.getColumnCount() - 1 ? SwingConstants.CENTER
                        : SwingConstants.TRAILING));
        Renderer r = new Renderer();
        for (int i = 0; i < model.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(i == 1 ? new PersonRenderer() : r);
            if (model.isComponent(i)) table.getColumnModel().getColumn(i).setCellEditor(ed);
        }
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(230);
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(null);
        Card card = new Card(new MigLayout("insets 0, fill, wrap 1", "[grow, fill]", "[grow, fill]10[]"));
        card.pad(6, 6, 12, 6);
        card.add(sp, "h " + Math.min(620, 60 + gb.roster().size() * 44) + "!");
        JPanel foot = new JPanel(new MigLayout("insets 0 12 0 8, fillx", "[]push[][]", "[center]"));
        foot.setOpaque(false);
        foot.add(dirtyLabel);
        foot.add(discard);
        foot.add(save);
        card.add(foot, "growx");
        col.add(card);
        updateDirty();
        setBody(col, true);
    }

    private void updateDirty() {
        int n = model == null ? 0 : model.dirtyCount();
        int bad = model == null ? 0 : model.invalidCount();
        dirtyLabel.setText(bad > 0 ? "● " + bad + " invalid mark" + (bad == 1 ? "" : "s") + " — fix before saving"
                : n == 0 ? "All changes saved" : "● " + n + " unsaved change" + (n == 1 ? "" : "s"));
        dirtyLabel.setForeground(bad > 0 ? Theme.p().danger() : n > 0 ? Theme.p().warning() : Theme.p().muted());
        save.setEnabled(n > 0 && bad == 0 && !shell.maintenance());
        discard.setEnabled(n > 0);
    }

    private void saveChanges() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();
        Map<Integer, Map<String, Double>> changes = model.changes();
        Async.button(save, () -> InstructorService.saveScores(gb.section().id(), changes), n -> {
            Toasts.success(this, "Saved " + n + " mark" + (n == 1 ? "" : "s") + ".");
            reload();
        });
    }

    private void publish() {
        if (model.dirtyCount() > 0) {
            Toasts.warning(this, "Save your changes before publishing.");
            return;
        }
        long complete = gb.roster().stream()
                .filter(r -> InstructorService.finalPercent(gb.components(), gb.scores().get(r.enrollmentId())) != null).count();
        Modal.confirm(this, "Publish final grades?", "Grades will be published for <b>" + complete + " of "
                + gb.roster().size() + "</b> students who have every assessment marked. Students see them immediately, "
                + "and they update automatically if you change marks later.", "Publish", false,
                () -> Async.run(this, () -> InstructorService.computeFinals(gb.section().id()), res -> {
                    Toasts.success(this, "Published " + res.computed() + " final grade" + (res.computed() == 1 ? "" : "s")
                            + (res.incomplete() > 0 ? "; " + res.incomplete() + " still have unmarked assessments." : "."));
                    reload();
                }));
    }

    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(System.getProperty("user.home"), gb.section().courseCode() + "-section"
                + gb.section().id() + "-grades.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        var path = fc.getSelectedFile().toPath();
        Async.run(this, () -> {
            try {
                InstructorService.exportCsv(gb.section().id(), path);
            } catch (java.io.IOException e) {
                throw new RuntimeException("Couldn't write the file: " + e.getMessage(), e);
            }
            return path;
        }, p -> Toasts.success(this, "Exported to " + p.getFileName() + "."));
    }

    // ---- scheme editor ----------------------------------------------------------------------

    private void editScheme() {
        Modal m = new Modal(this, "Assessment scheme", gb.section().courseCode() + " · weights must add up to 100%. "
                + "Removing or renaming an assessment deletes its recorded marks.", "sliders").width(620);
        List<JTextField[]> rows = new ArrayList<>();
        JPanel list = new JPanel(new MigLayout("insets 0, fillx, wrap 4, gap 10", "[grow, fill][110!][110!][40!]", ""));
        list.setOpaque(false);
        JLabel total = new JLabel();
        total.setFont(Theme.semibold(13f));
        Runnable recompute = () -> {
            double sum = 0;
            for (JTextField[] r : rows) {
                try {
                    sum += Double.parseDouble(r[2].getText().trim());
                } catch (NumberFormatException ignored) {
                    // incomplete row
                }
            }
            boolean ok = Math.abs(sum - 100) < 0.01;
            total.setText((ok ? "✓ " : "") + "Total weight " + fmt(sum) + "%");
            total.setForeground(ok ? Theme.p().success() : Theme.p().warning());
        };
        Runnable[] rebuild = new Runnable[1];
        rebuild[0] = () -> {
            list.removeAll();
            for (String h : new String[]{"Assessment", "Out of", "Weight %", ""}) {
                JLabel l = UI.muted(h.toUpperCase());
                l.setFont(Theme.semibold(10.5f));
                list.add(l);
            }
            for (JTextField[] r : rows) {
                list.add(r[0], "h 40!");
                list.add(r[1], "h 40!");
                list.add(r[2], "h 40!");
                Btn del = Btn.icon("trash", "Remove");
                del.addActionListener(e -> {
                    rows.remove(r);
                    rebuild[0].run();
                });
                list.add(del, "h 36!");
            }
            recompute.run();
            list.revalidate();
            list.repaint();
            m.width(620);
        };
        javax.swing.event.DocumentListener dl = new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                recompute.run();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                recompute.run();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
            }
        };
        java.util.function.BiFunction<String[], Boolean, JTextField[]> make = (vals, x) -> {
            JTextField n = UI.field("e.g. Quiz", null);
            JTextField mx = UI.field("20", null);
            JTextField w = UI.field("20", null);
            n.setText(vals[0]);
            mx.setText(vals[1]);
            w.setText(vals[2]);
            w.getDocument().addDocumentListener(dl);
            return new JTextField[]{n, mx, w};
        };
        for (GradeComponent c : gb.components()) rows.add(make.apply(new String[]{c.name(), fmt(c.maxScore()), fmt(c.weight())}, true));
        rebuild[0].run();
        JPanel body = new JPanel(new MigLayout("insets 0, fillx, wrap 1", "[grow, fill]", "[]12[]"));
        body.setOpaque(false);
        body.add(list);
        JPanel foot = new JPanel(new MigLayout("insets 0, fillx", "[]push[]", "[center]"));
        foot.setOpaque(false);
        Btn add = Btn.ghost("Add assessment", "plus");
        add.addActionListener(e -> {
            rows.add(make.apply(new String[]{"", "", ""}, true));
            rebuild[0].run();
        });
        foot.add(add);
        foot.add(total);
        body.add(foot);
        m.content(body);
        m.cancel("Cancel");
        Btn ok = Btn.primary("Save scheme", "check");
        ok.addActionListener(e -> {
            List<GradeComponent> comps = new ArrayList<>();
            try {
                for (JTextField[] r : rows) {
                    comps.add(new GradeComponent(0, gb.section().id(), r[0].getText().trim(),
                            Double.parseDouble(r[1].getText().trim()), Double.parseDouble(r[2].getText().trim()), 0));
                }
            } catch (NumberFormatException ex) {
                Toasts.error(this, "Marks and weights must be numbers.");
                return;
            }
            Async.button(ok, () -> {
                InstructorService.saveComponents(gb.section().id(), comps);
                return true;
            }, done -> {
                m.close();
                Toasts.success(this, "Assessment scheme updated.");
                reload();
            });
        });
        m.primary(ok);
        m.show();
    }

    // ---- table model ------------------------------------------------------------------------

    private final class Model extends AbstractTableModel {
        private final List<RosterEntry> rows = gb.roster();
        private final List<GradeComponent> comps = gb.components();
        private final Map<String, Object> edits = new HashMap<>();

        private String key(int r, int c) {
            return r + ":" + c;
        }

        boolean isComponent(int c) {
            return c >= 2 && c < 2 + comps.size();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return comps.size() + 4;
        }

        @Override
        public String getColumnName(int c) {
            if (c == 0) return "ROLL NO";
            if (c == 1) return "STUDENT";
            if (isComponent(c)) {
                GradeComponent g = comps.get(c - 2);
                return g.name().toUpperCase() + "  /" + fmt(g.maxScore()) + " · " + fmt(g.weight()) + "%";
            }
            return c == comps.size() + 2 ? "WEIGHTED %" : "GRADE";
        }

        @Override
        public boolean isCellEditable(int r, int c) {
            return isComponent(c) && !shell.maintenance();
        }

        Double stored(int r, int c) {
            GradeEntry g = gb.scores().getOrDefault(rows.get(r).enrollmentId(), Map.of()).get(comps.get(c - 2).name());
            return g == null ? null : g.score();
        }

        Object cell(int r, int c) {
            String k = key(r, c);
            return edits.containsKey(k) ? edits.get(k) : stored(r, c);
        }

        boolean isDirty(int r, int c) {
            return edits.containsKey(key(r, c));
        }

        boolean isInvalid(int r, int c) {
            Object v = cell(r, c);
            if (v == null) return false;
            if (!(v instanceof Double d)) return true;
            return d < 0 || d > comps.get(c - 2).maxScore();
        }

        int dirtyCount() {
            return edits.size();
        }

        int invalidCount() {
            int n = 0;
            for (String k : edits.keySet()) {
                String[] p = k.split(":");
                if (isInvalid(Integer.parseInt(p[0]), Integer.parseInt(p[1]))) n++;
            }
            return n;
        }

        Double weighted(int r) {
            double total = 0;
            double weights = 0;
            for (int i = 0; i < comps.size(); i++) {
                Object v = cell(r, i + 2);
                if (v instanceof Double d && !isInvalid(r, i + 2)) {
                    total += d / comps.get(i).maxScore() * comps.get(i).weight();
                    weights += comps.get(i).weight();
                }
            }
            return weights == 0 ? null : total / weights * 100;
        }

        @Override
        public Object getValueAt(int r, int c) {
            RosterEntry e = rows.get(r);
            if (c == 0) return e.rollNo();
            if (c == 1) return new String[]{e.name(), e.program()};
            if (isComponent(c)) return cell(r, c);
            if (c == comps.size() + 2) return weighted(r);
            return e.finalGrade();
        }

        @Override
        public void setValueAt(Object v, int r, int c) {
            String s = v == null ? "" : v.toString().trim();
            Object parsed;
            if (s.isEmpty()) parsed = null;
            else {
                try {
                    parsed = Double.parseDouble(s);
                } catch (NumberFormatException e) {
                    parsed = s;
                }
            }
            Double orig = stored(r, c);
            boolean same = parsed == null ? orig == null : parsed instanceof Double d && orig != null && Math.abs(d - orig) < 1e-9;
            if (same) edits.remove(key(r, c));
            else edits.put(key(r, c), parsed);
            fireTableRowsUpdated(r, r);
            updateDirty();
        }

        Map<Integer, Map<String, Double>> changes() {
            Map<Integer, Map<String, Double>> out = new HashMap<>();
            for (var en : edits.entrySet()) {
                String[] p = en.getKey().split(":");
                int r = Integer.parseInt(p[0]);
                int c = Integer.parseInt(p[1]);
                out.computeIfAbsent(rows.get(r).enrollmentId(), k -> new HashMap<>())
                        .put(comps.get(c - 2).name(), (Double) en.getValue());
            }
            return out;
        }
    }

    private final class Renderer extends DefaultTableCellRenderer {
        private boolean dirty;
        private boolean invalid;
        private String grade;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
            grade = null;
            dirty = model.isComponent(c) && model.isDirty(r, c);
            invalid = model.isComponent(c) && model.isInvalid(r, c);
            String text;
            if (c == model.getColumnCount() - 1) {
                grade = (String) v;
                text = "";
            } else if (v instanceof Double d) {
                text = c == model.getColumnCount() - 2 ? String.format("%.1f%%", d) : fmt(d);
            } else {
                text = v == null ? (model.isComponent(c) ? "–" : "—") : v.toString();
            }
            super.getTableCellRendererComponent(t, text, sel, false, r, c);
            setHorizontalAlignment(c == 0 ? LEFT : c == model.getColumnCount() - 1 ? CENTER : RIGHT);
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 16));
            Theme.Palette p = Theme.p();
            setFont(c == model.getColumnCount() - 2 ? Theme.semibold(13f) : Theme.font(13f));
            Color bg = sel ? p.accentSoft() : r % 2 == 1 ? p.surfaceAlt() : p.surface();
            if (invalid) bg = Theme.alpha(p.danger(), 0.16);
            else if (dirty) bg = Theme.alpha(p.warning(), Theme.isDark() ? 0.18 : 0.14);
            setBackground(bg);
            setForeground(invalid ? p.danger() : v == null ? p.subtle() : p.text());
            if (c == model.getColumnCount() - 2 && v instanceof Double d) setForeground(Parts.percentColor(d));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = Theme.hq(g);
            if (grade != null) {
                int w = g2.getFontMetrics(Theme.semibold(11.5f)).stringWidth(grade) + 18;
                Badge.paint(g2, grade, Parts.gradeColor(grade), (getWidth() - w) / 2, (getHeight() - 22) / 2, false,
                        Theme.semibold(11.5f));
            } else if (dirty && !invalid) {
                g2.setColor(Theme.p().warning());
                g2.fill(new Ellipse2D.Double(6, getHeight() / 2.0 - 3, 6, 6));
            }
            g2.dispose();
        }
    }

    private final class PersonRenderer extends JComponent implements TableCellRenderer {
        private String[] v;
        private Color bg;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean sel, boolean f, int r, int c) {
            v = (String[]) value;
            Theme.Palette p = Theme.p();
            bg = sel ? p.accentSoft() : r % 2 == 1 ? p.surfaceAlt() : p.surface();
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.hq(g);
            g2.setColor(bg);
            g2.fillRect(0, 0, getWidth(), getHeight());
            Avatar.paint(g2, v[0], 12, (getHeight() - 28) / 2, 28);
            g2.setFont(Theme.semibold(13f));
            g2.setColor(Theme.p().text());
            g2.drawString(v[0], 50, getHeight() / 2 - 1);
            g2.setFont(Theme.font(11.5f));
            g2.setColor(Theme.p().muted());
            g2.drawString(v[1] == null ? "" : v[1], 50, getHeight() / 2 + 14);
            g2.dispose();
        }
    }

    static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format("%.1f", v);
    }
}
