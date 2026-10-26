package edu.univ.erp.ui.page.admin;

import edu.univ.erp.domain.InstructorProfile;
import edu.univ.erp.domain.Role;
import edu.univ.erp.domain.StudentProfile;
import edu.univ.erp.domain.User;
import edu.univ.erp.service.AdminService;
import edu.univ.erp.ui.kit.Async;
import edu.univ.erp.ui.kit.Btn;
import edu.univ.erp.ui.kit.Card;
import edu.univ.erp.ui.kit.DataTable;
import edu.univ.erp.ui.kit.DropdownMenu;
import edu.univ.erp.ui.kit.Modal;
import edu.univ.erp.ui.kit.Segmented;
import edu.univ.erp.ui.kit.Select;
import edu.univ.erp.ui.kit.Toasts;
import edu.univ.erp.ui.kit.UI;
import edu.univ.erp.ui.page.Page;
import edu.univ.erp.ui.page.Parts;
import edu.univ.erp.ui.page.Shell;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Accounts for every role. */
public class UsersPage extends Page {

    private record Data(List<User> users, Map<Integer, StudentProfile> students, Map<Integer, InstructorProfile> instructors,
                        List<String> departments) {
    }

    private Data data;
    private final DataTable<User> table = new DataTable<>();
    private final Segmented roles = new Segmented("All", "Students", "Instructors", "Admins");
    private final JTextField search = UI.search("Search name, username, roll number or email");
    private final Btn edit = Btn.secondary("Edit", "edit");
    private final Btn reset = Btn.secondary("Reset password", "key");
    private final Btn lock = Btn.secondary("Lock", "lock");
    private final Btn delete = Btn.ghost("Delete", "trash");

    public UsersPage(Shell shell) {
        super(shell);
        table.person("Name", 260, User::displayName, u -> "@" + u.username())
                .badge("Role", 140, u -> u.role().label(), Parts::statusColor)
                .badge("Status", 120, u -> u.isLocked() ? "Locked" : "Active", v -> Parts.statusColor(
                        "Locked".equals(v) ? "LOCKED" : "ACTIVE"))
                .col("Details", 220, this::details)
                .col("Email", 220, User::email)
                .col("Last sign-in", 170, u -> Parts.dateTime(u.lastLogin()))
                .search(u -> u.displayName() + " " + u.username() + " " + u.email() + " " + details(u))
                .empty("users", "No users match", "Try another search or filter.")
                .onDoubleClick(this::openEdit)
                .onSelect(u -> updateButtons())
                .menu(this::menu);
        roles.onChange(i -> applyFilter());
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        edit.addActionListener(e -> withSelected(this::openEdit));
        reset.addActionListener(e -> withSelected(this::openReset));
        lock.addActionListener(e -> withSelected(this::toggleLock));
        delete.addActionListener(e -> withSelected(this::confirmDelete));
        delete.putClientProperty("fg", edu.univ.erp.ui.theme.Theme.p().danger());
    }

    @Override
    public String title() {
        return "Users";
    }

    @Override
    public String subtitle() {
        return data == null ? "Accounts and access" : data.users().size() + " accounts";
    }

    @Override
    public void reload() {
        load(() -> {
            Map<Integer, StudentProfile> s = new HashMap<>();
            for (StudentProfile p : AdminService.students()) s.put(p.userId(), p);
            Map<Integer, InstructorProfile> i = new HashMap<>();
            for (InstructorProfile p : AdminService.instructors()) i.put(p.userId(), p);
            return new Data(AdminService.users(), s, i, AdminService.departments());
        }, d -> {
            data = d;
            shell.refreshTitle(this);
            JPanel col = column();
            JPanel bar = new JPanel(new MigLayout("insets 0, fillx", "[][grow, fill]push[]", "[center]"));
            bar.setOpaque(false);
            bar.add(roles, "h 38!");
            bar.add(search, "h 40!, gapleft 12, wmax 420");
            Btn add = Btn.primary("Add user", "plus");
            add.addActionListener(e -> openCreate());
            bar.add(add);
            col.add(bar);
            Card c = new Card(new MigLayout("insets 0, fill, wrap 1", "[grow, fill]", "[]8[grow, fill]"));
            c.pad(12, 10, 8, 10);
            JPanel actions = new JPanel(new MigLayout("insets 0 6 0 6, fillx", "[]push[][][][]", "[center]"));
            actions.setOpaque(false);
            actions.add(UI.muted("Select a user for actions, double-click to edit, or right-click for a menu."));
            actions.add(edit);
            actions.add(reset);
            actions.add(lock);
            actions.add(delete);
            c.add(actions, "growx");
            c.add(table, "h 600!");
            col.add(c);
            table.setRows(d.users());
            applyFilter();
            updateButtons();
            setBody(col, true);
        });
    }

    private String details(User u) {
        if (data == null) return "";
        if (u.role() == Role.STUDENT) {
            StudentProfile p = data.students().get(u.id());
            return p == null ? "" : p.rollNo() + " · " + (p.program() == null ? "" : p.program());
        }
        if (u.role() == Role.INSTRUCTOR) {
            InstructorProfile p = data.instructors().get(u.id());
            return p == null ? "" : (p.department() == null ? "" : p.department()) + (p.office() == null ? "" : " · " + p.office());
        }
        return "Full access";
    }

    private void applyFilter() {
        String q = search.getText().trim().toLowerCase();
        int r = roles.selected();
        table.filter(u -> (r == 0 || r == 1 && u.role() == Role.STUDENT || r == 2 && u.role() == Role.INSTRUCTOR
                || r == 3 && u.role() == Role.ADMIN)
                && (q.isEmpty() || (u.displayName() + " " + u.username() + " " + u.email() + " " + details(u))
                .toLowerCase().contains(q)));
    }

    private void updateButtons() {
        User u = table.selected();
        boolean has = u != null;
        boolean self = has && u.id() == shell.user().id();
        edit.setEnabled(has);
        reset.setEnabled(has);
        lock.setEnabled(has && !self);
        delete.setEnabled(has && !self);
        lock.setText(has && u.isLocked() ? "Unlock" : "Lock");
        lock.setIconName(has && u.isLocked() ? "unlock" : "lock");
        lock.revalidate();
    }

    private void withSelected(java.util.function.Consumer<User> f) {
        User u = table.selected();
        if (u != null) f.accept(u);
    }

    private List<DropdownMenu.Item> menu(User u) {
        List<DropdownMenu.Item> items = new ArrayList<>();
        items.add(DropdownMenu.Item.of("edit", "Edit details", () -> openEdit(u)));
        items.add(DropdownMenu.Item.of("key", "Reset password", () -> openReset(u)));
        if (u.id() != shell.user().id()) {
            items.add(DropdownMenu.Item.of(u.isLocked() ? "unlock" : "lock", u.isLocked() ? "Unlock account" : "Lock account",
                    () -> toggleLock(u)));
            items.add(DropdownMenu.Item.separator());
            items.add(DropdownMenu.Item.danger("trash", "Delete account", () -> confirmDelete(u)));
        }
        return items;
    }

    // ---- create / edit ----------------------------------------------------------------------

    private void openCreate() {
        form(null);
    }

    private void openEdit(User u) {
        form(u);
    }

    private void form(User existing) {
        boolean create = existing == null;
        Modal m = new Modal(this, create ? "Add user" : "Edit " + existing.displayName(),
                create ? "Creates the login and profile together." : "@" + existing.username() + " · " + existing.role().label(),
                create ? "plus" : "edit").width(600);
        JPanel f = new JPanel(new MigLayout("insets 0, fillx, wrap 2, gap 14", "[grow, fill, sg a][grow, fill, sg a]", ""));
        f.setOpaque(false);
        Select<Role> role = new Select<Role>().labels(Role::label).options(List.of(Role.STUDENT, Role.INSTRUCTOR, Role.ADMIN));
        role.setSelectedQuietly(create ? Role.STUDENT : existing.role());
        role.setEnabled(create);
        JTextField username = UI.field("e.g. jdoe", "user");
        JTextField name = UI.field("Full name", null);
        JTextField email = UI.field("name@campus.edu", "mail");
        JPasswordField pw = UI.password("8+ characters, letters and numbers");
        JTextField roll = UI.field("e.g. 2026101", null);
        JTextField program = UI.field("e.g. B.Tech CSE", null);
        JTextField year = UI.field("e.g. 2026", null);
        Select<String> dept = new Select<String>().placeholder("Department").options(data.departments());
        JTextField deptOther = UI.field("…or type a new department", null);
        JTextField office = UI.field("e.g. A-305", null);
        if (!create) {
            username.setText(existing.username());
            username.setEnabled(false);
            name.setText(existing.fullName() == null ? "" : existing.fullName());
            email.setText(existing.email() == null ? "" : existing.email());
            StudentProfile sp = data.students().get(existing.id());
            if (sp != null) {
                roll.setText(sp.rollNo());
                program.setText(sp.program() == null ? "" : sp.program());
                year.setText(String.valueOf(sp.year()));
            }
            InstructorProfile ip = data.instructors().get(existing.id());
            if (ip != null) {
                if (data.departments().contains(ip.department())) dept.setSelectedQuietly(ip.department());
                else deptOther.setText(ip.department() == null ? "" : ip.department());
                office.setText(ip.office() == null ? "" : ip.office());
            }
        }
        JPanel studentFields = UI.clear(new MigLayout("insets 0, fillx, wrap 3, gap 14", "[grow, fill][grow, fill][120!, fill]", ""));
        studentFields.add(UI.labeled("Roll number", roll, null));
        studentFields.add(UI.labeled("Program", program, null));
        studentFields.add(UI.labeled("Batch", year, null));
        JPanel instructorFields = UI.clear(new MigLayout("insets 0, fillx, wrap 3, gap 14", "[grow, fill][grow, fill][120!, fill]", ""));
        instructorFields.add(UI.labeled("Department", dept, null));
        instructorFields.add(UI.labeled("New department", deptOther, null));
        instructorFields.add(UI.labeled("Office", office, null));

        f.add(UI.labeled("Role", role, null));
        f.add(UI.labeled("Username", username, null));
        f.add(UI.labeled("Full name", name, null));
        f.add(UI.labeled("Email", email, null));
        if (create) f.add(UI.labeled("Password", pw, "They can change it after signing in"), "span 2");
        f.add(studentFields, "span 2, hidemode 3");
        f.add(instructorFields, "span 2, hidemode 3");
        Runnable sync = () -> {
            Role r = role.selected();
            studentFields.setVisible(r == Role.STUDENT);
            instructorFields.setVisible(r == Role.INSTRUCTOR);
            f.revalidate();
            f.repaint();
            m.width(600);
        };
        role.onChange(r -> sync.run());
        sync.run();
        m.content(f);
        m.cancel("Cancel");
        Btn ok = Btn.primary(create ? "Create user" : "Save changes", "check");
        ok.addActionListener(e -> {
            Integer y = null;
            if (role.selected() == Role.STUDENT) {
                try {
                    y = Integer.parseInt(year.getText().trim());
                } catch (NumberFormatException ex) {
                    Toasts.error(this, "Batch must be a year such as 2026.");
                    return;
                }
            }
            String d = deptOther.getText().isBlank() ? dept.selected() : deptOther.getText().trim();
            AdminService.UserForm form = new AdminService.UserForm(username.getText(), new String(pw.getPassword()),
                    name.getText(), email.getText(), role.selected(), roll.getText(), program.getText(), y, d, office.getText());
            Async.button(ok, () -> {
                if (create) AdminService.createUser(form);
                else AdminService.updateUser(existing.id(), form);
                return true;
            }, done -> {
                m.close();
                Toasts.success(this, create ? "Created @" + username.getText().trim() + "." : "Saved changes.");
                reload();
            });
        });
        m.primary(ok);
        m.show();
    }

    private void openReset(User u) {
        Modal m = new Modal(this, "Reset password", "For " + u.displayName() + " (@" + u.username() + "). "
                + "This also unlocks the account.", "key").width(500);
        JPanel f = new JPanel(new MigLayout("insets 0, fillx, wrap 1, gap 12", "[grow, fill]", ""));
        f.setOpaque(false);
        JTextField pw = UI.field("New password", "lock");
        Btn gen = Btn.ghost("Generate a strong one", "sparkle");
        gen.addActionListener(e -> {
            pw.setText(generate());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(pw.getText()), null);
            Toasts.info(this, "Generated and copied to the clipboard.");
        });
        f.add(UI.labeled("New password", pw, "Share it securely; they can change it from their profile"));
        f.add(gen, "w pref!");
        m.content(f);
        m.cancel("Cancel");
        Btn ok = Btn.primary("Reset password", "check");
        ok.addActionListener(e -> Async.button(ok, () -> {
            AdminService.resetPassword(u.id(), pw.getText());
            return true;
        }, done -> {
            m.close();
            Toasts.success(this, "Password reset for @" + u.username() + ".");
            reload();
        }));
        m.primary(ok);
        m.show();
    }

    private static String generate() {
        String letters = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
        String digits = "23456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) sb.append(letters.charAt(r.nextInt(letters.length())));
        sb.insert(r.nextInt(sb.length()), digits.charAt(r.nextInt(digits.length())));
        sb.insert(r.nextInt(sb.length()), digits.charAt(r.nextInt(digits.length())));
        return sb.toString();
    }

    private void toggleLock(User u) {
        boolean locking = !u.isLocked();
        Runnable go = () -> Async.run(this, () -> {
            AdminService.setLocked(u.id(), locking);
            return true;
        }, ok -> {
            Toasts.success(this, (locking ? "Locked" : "Unlocked") + " @" + u.username() + ".");
            reload();
        });
        if (locking) {
            Modal.confirm(this, "Lock @" + u.username() + "?", u.displayName() + " won't be able to sign in until unlocked.",
                    "Lock account", true, go);
        } else {
            go.run();
        }
    }

    private void confirmDelete(User u) {
        Async.run(this, () -> u.role() == Role.STUDENT ? AdminService.enrollmentCount(u.id()) : 0, n -> {
            String extra = n > 0 ? " Their <b>" + n + " registration record(s)</b> and marks will be deleted too." : "";
            Modal.confirm(this, "Delete @" + u.username() + "?", "This permanently removes " + UI.esc(u.displayName())
                    + "'s account." + extra + " Consider locking the account instead if you might need the history.",
                    "Delete permanently", true, () -> Async.run(this, () -> {
                        AdminService.deleteUser(u.id());
                        return true;
                    }, ok -> {
                        Toasts.success(this, "Deleted @" + u.username() + ".");
                        reload();
                    }));
        });
    }
}
