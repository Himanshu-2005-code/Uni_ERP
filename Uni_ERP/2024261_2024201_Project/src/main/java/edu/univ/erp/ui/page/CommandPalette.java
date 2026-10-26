package edu.univ.erp.ui.page;

import com.formdev.flatlaf.FlatClientProperties;
import edu.univ.erp.ui.kit.Icons;
import edu.univ.erp.ui.kit.Modal;
import edu.univ.erp.ui.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/** Ctrl+K: fuzzy-jump to any page or run a quick action. */
final class CommandPalette {

    record Command(String icon, String label, String hint, Runnable run) {
    }

    private CommandPalette() {
    }

    static void open(Component owner, List<Command> commands) {
        Modal m = new Modal(owner, "Quick jump", "Type to search pages and actions. Enter to run, Esc to close.",
                "command").width(560);
        JPanel body = new JPanel(new MigLayout("insets 0, fill, wrap 1", "[grow, fill]", "[]10[grow, fill]"));
        body.setOpaque(false);
        JTextField q = new JTextField();
        q.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search…");
        q.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, Icons.of("search", 16, () -> Theme.p().muted()));
        q.setFont(Theme.font(14f));
        body.add(q, "h 44!");
        DefaultListModel<Command> model = new DefaultListModel<>();
        JList<Command> list = new JList<>(model);
        list.setCellRenderer(new Row());
        list.setOpaque(false);
        list.setFixedCellHeight(46);
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        body.add(sp, "h 330!");
        Runnable refill = () -> {
            model.clear();
            String s = q.getText().trim().toLowerCase();
            for (Command c : commands) {
                if (s.isEmpty() || fuzzy(c.label().toLowerCase(), s) || c.hint().toLowerCase().contains(s)) model.addElement(c);
            }
            if (!model.isEmpty()) list.setSelectedIndex(0);
        };
        refill.run();
        q.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refill.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refill.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        Runnable go = () -> {
            Command c = list.getSelectedValue();
            if (c == null) return;
            m.close();
            javax.swing.SwingUtilities.invokeLater(c.run());
        };
        q.getInputMap().put(KeyStroke.getKeyStroke("DOWN"), "down");
        q.getActionMap().put("down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int i = Math.min(model.size() - 1, list.getSelectedIndex() + 1);
                list.setSelectedIndex(i);
                list.ensureIndexIsVisible(i);
            }
        });
        q.getInputMap().put(KeyStroke.getKeyStroke("UP"), "up");
        q.getActionMap().put("up", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int i = Math.max(0, list.getSelectedIndex() - 1);
                list.setSelectedIndex(i);
                list.ensureIndexIsVisible(i);
            }
        });
        q.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "go");
        q.getActionMap().put("go", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                go.run();
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                go.run();
            }
        });
        m.content(body);
        m.show();
    }

    private static boolean fuzzy(String text, String q) {
        int i = 0;
        for (char ch : text.toCharArray()) if (i < q.length() && ch == q.charAt(i)) i++;
        return i == q.length();
    }

    private static final class Row extends JComponent implements ListCellRenderer<Command> {
        private Command c;
        private boolean sel;

        @Override
        public Component getListCellRendererComponent(JList<? extends Command> list, Command value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            c = value;
            sel = isSelected;
            return this;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(400, 46);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.hq(g);
            Theme.Palette p = Theme.p();
            if (sel) {
                g2.setColor(Theme.isDark() ? Theme.alpha(java.awt.Color.WHITE, 0.07) : p.accentSoft());
                g2.fill(new RoundRectangle2D.Double(0, 2, getWidth(), getHeight() - 4, 12, 12));
            }
            Icons.paint(g2, c.icon(), 14, (getHeight() - 18) / 2.0, 18, sel ? p.accent() : p.muted(), 1.9f);
            g2.setFont(Theme.medium(13.5f));
            g2.setColor(p.text());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(c.label(), 46, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.setFont(Theme.font(11.5f));
            g2.setColor(p.subtle());
            FontMetrics fh = g2.getFontMetrics();
            g2.drawString(c.hint(), getWidth() - fh.stringWidth(c.hint()) - 14, (getHeight() - fh.getHeight()) / 2 + fh.getAscent());
            g2.dispose();
        }
    }
}
