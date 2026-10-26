package edu.univ.erp.ui.page.admin;

import edu.univ.erp.service.SettingsService;
import edu.univ.erp.ui.kit.Async;
import edu.univ.erp.ui.kit.Btn;
import edu.univ.erp.ui.kit.DateField;
import edu.univ.erp.ui.kit.Modal;
import edu.univ.erp.ui.kit.Select;
import edu.univ.erp.ui.kit.Toasts;
import edu.univ.erp.ui.kit.Toggle;
import edu.univ.erp.ui.kit.UI;
import edu.univ.erp.ui.page.Page;
import edu.univ.erp.ui.page.Parts;
import edu.univ.erp.ui.page.Shell;
import edu.univ.erp.ui.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.List;

/** Term, deadlines, limits and maintenance mode. */
public class SettingsPage extends Page {

    public SettingsPage(Shell shell) {
        super(shell);
    }

    @Override
    public String title() {
        return "Academic settings";
    }

    @Override
    public String subtitle() {
        return "Term, deadlines and system switches";
    }

    @Override
    public void reload() {
        load(() -> {
            SettingsService s = SettingsService.get().refresh();
            s.maintenance();
            return s;
        }, this::render);
    }

    private void render(SettingsService s) {
        JPanel col = column();
        col.add(maintenanceCard(s.maintenance()));

        JPanel f = new JPanel(new MigLayout("insets 0, fillx, wrap 2, gap 16 14", "[grow, fill, sg a][grow, fill, sg a]", "[top]"));
        f.setOpaque(false);
        JTextField inst = UI.field("Institution name", "building");
        inst.setText(s.institution());
        Select<String> sem = new Select<String>().options(List.of("Monsoon", "Winter", "Summer")).leading("calendar");
        sem.setSelectedQuietly(s.semester());
        JTextField year = UI.field("2026", null);
        year.setText(String.valueOf(s.year()));
        DateField add = new DateField();
        add.setDate(s.addDeadline());
        DateField drop = new DateField();
        drop.setDate(s.dropDeadline());
        JTextField credits = UI.field("24", null);
        credits.setText(String.valueOf(s.maxCredits()));
        JTextField lockout = UI.field("5", null);
        lockout.setText(String.valueOf(s.lockoutThreshold()));
        f.add(UI.labeled("Institution", inst, "Shown on sign-in and transcripts"), "span 2");
        f.add(UI.labeled("Current semester", sem, "Students register for this term"));
        f.add(UI.labeled("Year", year, "Academic year of the current term"));
        f.add(UI.labeled("Add deadline", add, "Last day to register"));
        f.add(UI.labeled("Drop deadline", drop, "Last day to drop a course"));
        f.add(UI.labeled("Credit limit per term", credits, "Registrations above this are blocked"));
        f.add(UI.labeled("Lock accounts after", lockout, "Consecutive wrong passwords"));
        Btn save = Btn.primary("Save settings", "check");
        save.addActionListener(e -> {
            int y;
            int cr;
            int lo;
            try {
                y = Integer.parseInt(year.getText().trim());
                cr = Integer.parseInt(credits.getText().trim());
                lo = Integer.parseInt(lockout.getText().trim());
            } catch (NumberFormatException ex) {
                Toasts.error(this, "Year, credit limit and lockout must be whole numbers.");
                return;
            }
            Async.button(save, () -> {
                SettingsService.get().update(inst.getText(), sem.selected(), y, add.date(), drop.date(), cr, lo);
                return true;
            }, ok -> {
                Toasts.success(this, "Settings saved.");
                shell.refreshTitle(this);
            });
        });
        col.add(Parts.section("Academic term", "Changes apply immediately for everyone", f, save));
        setBody(col, true);
    }

    private JComponent maintenanceCard(boolean on) {
        JPanel p = new JPanel(new MigLayout("insets 0, fillx", "[]16[grow]push[]", "[center]"));
        p.setOpaque(false);
        p.add(Parts.bubble("server", on ? Theme.p().warning() : Theme.p().success(), 48));
        JPanel text = UI.clear(new MigLayout("insets 0, wrap 1, gap 4", "", ""));
        text.add(UI.h2(on ? "Maintenance mode is on" : "System is live"));
        text.add(UI.html("<div style='width:431px'>" + (on
                ? "Students and instructors can sign in and browse, but registrations, drops and grade changes are paused."
                : "Turn on maintenance mode before term rollovers or bulk changes. Everyone else becomes read-only; "
                + "administrators keep full access.") + "</div>", 12.5f, true));
        p.add(text);
        Toggle t = new Toggle(on);
        t.onChange(v -> {
            Runnable apply = () -> Async.run(this, () -> {
                SettingsService.get().setMaintenance(v);
                return v;
            }, done -> {
                Toasts.success(this, done ? "Maintenance mode is on. Everyone else is read-only." : "System is live again.");
                shell.pollStatus();
                reload();
            });
            if (v) {
                Modal.confirm(this, "Turn on maintenance mode?", "Students and instructors won't be able to make changes "
                        + "until you turn it off.", "Turn on", true, apply);
                t.setOn(false, false);
            } else {
                apply.run();
            }
        });
        p.add(t);
        return Parts.section("Maintenance", null, p);
    }
}
