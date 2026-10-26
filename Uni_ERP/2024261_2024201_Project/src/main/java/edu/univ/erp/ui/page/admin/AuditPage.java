package edu.univ.erp.ui.page.admin;

import edu.univ.erp.domain.AuditEntry;
import edu.univ.erp.service.AdminService;
import edu.univ.erp.ui.kit.Btn;
import edu.univ.erp.ui.kit.Card;
import edu.univ.erp.ui.kit.DataTable;
import edu.univ.erp.ui.kit.Select;
import edu.univ.erp.ui.kit.UI;
import edu.univ.erp.ui.page.Page;
import edu.univ.erp.ui.page.Parts;
import edu.univ.erp.ui.page.Shell;
import edu.univ.erp.ui.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Who did what, and when. */
public class AuditPage extends Page {

    private static final String ALL = "All actions";
    private final DataTable<AuditEntry> table = new DataTable<>();
    private final JTextField search = UI.search("Search user or details");
    private final Select<String> action = new Select<String>().leading("filter");
    private int count;

    public AuditPage(Shell shell) {
        super(shell);
        table.col("When", 170, e -> Parts.dateTime(e.createdAt()))
                .person("User", 200, e -> e.username() == null ? "system" : e.username(), e -> Shell.ago(e.createdAt()))
                .badge("Action", 190, e -> label(e.action()), v -> colorOfLabel(String.valueOf(v)))
                .col("Details", 520, AuditEntry::details)
                .search(e -> e.username() + " " + e.action() + " " + e.details())
                .empty("history", "No activity", null);
        action.labels(a -> a.equals(ALL) ? ALL : label(a)).onChange(a -> apply());
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                apply();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                apply();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    @Override
    public String title() {
        return "Audit log";
    }

    @Override
    public String subtitle() {
        return "Latest " + count + " events";
    }

    @Override
    public void reload() {
        load(() -> AdminService.audit(500), list -> {
            count = list.size();
            shell.refreshTitle(this);
            TreeSet<String> acts = new TreeSet<>();
            for (AuditEntry e : list) acts.add(e.action());
            List<String> opts = new ArrayList<>();
            opts.add(ALL);
            opts.addAll(acts);
            action.options(opts);
            if (action.selected() == null) action.setSelectedQuietly(ALL);
            JPanel col = column();
            JPanel bar = new JPanel(new MigLayout("insets 0, fillx", "[240!][grow, fill]push[]", "[center]"));
            bar.setOpaque(false);
            bar.add(action, "h 40!");
            bar.add(search, "h 40!, gapleft 12, wmax 420");
            Btn refresh = Btn.secondary("Refresh", "refresh");
            refresh.addActionListener(e -> reload());
            bar.add(refresh);
            col.add(bar);
            Card c = new Card(new MigLayout("insets 0, fill", "[grow, fill]", "[grow, fill]"));
            c.pad(6, 6, 6, 6);
            c.add(table, "h 640!");
            col.add(c);
            table.setRows(list);
            apply();
            setBody(col, true);
        });
    }

    private void apply() {
        String a = action.selected();
        String q = search.getText().trim().toLowerCase();
        table.filter(e -> (a == null || a.equals(ALL) || e.action().equals(a))
                && (q.isEmpty() || (e.username() + " " + e.details() + " " + label(e.action())).toLowerCase().contains(q)));
    }

    public static String label(String action) {
        String s = action.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public static String icon(String a) {
        if (a.startsWith("LOGIN") || a.equals("LOGOUT")) return "key";
        if (a.startsWith("USER") || a.startsWith("PASSWORD")) return "user";
        if (a.startsWith("COURSE")) return "book";
        if (a.startsWith("SECTION") || a.equals("SCHEME_CHANGED")) return "calendar";
        if (a.startsWith("GRADES") || a.startsWith("FINALS")) return "award";
        if (a.equals("REGISTER") || a.equals("DROP")) return "layers";
        if (a.startsWith("ANNOUNCEMENT")) return "megaphone";
        if (a.equals("BACKUP") || a.equals("RESTORE")) return "database";
        return "sliders";
    }

    public static Color color(String a) {
        if (a.equals("LOGIN_FAILED") || a.endsWith("DELETED") || a.equals("USER_LOCKED") || a.equals("DROP")) return Theme.p().danger();
        if (a.equals("MAINTENANCE") || a.equals("RESTORE") || a.equals("PASSWORD_RESET")) return Theme.p().warning();
        if (a.endsWith("CREATED") || a.equals("REGISTER") || a.equals("FINALS_PUBLISHED")) return Theme.p().success();
        if (a.startsWith("LOGIN") || a.equals("LOGOUT")) return Theme.p().muted();
        return Theme.series(0);
    }

    private static Color colorOfLabel(String label) {
        return color(label.toUpperCase().replace(' ', '_'));
    }
}
