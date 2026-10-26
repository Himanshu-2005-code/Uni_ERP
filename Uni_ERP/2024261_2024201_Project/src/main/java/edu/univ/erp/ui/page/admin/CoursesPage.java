package edu.univ.erp.ui.page.admin;

import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.Section;
import edu.univ.erp.service.AdminService;
import edu.univ.erp.ui.kit.Async;
import edu.univ.erp.ui.kit.Btn;
import edu.univ.erp.ui.kit.Card;
import edu.univ.erp.ui.kit.DataTable;
import edu.univ.erp.ui.kit.DropdownMenu;
import edu.univ.erp.ui.kit.Modal;
import edu.univ.erp.ui.kit.Toasts;
import edu.univ.erp.ui.kit.UI;
import edu.univ.erp.ui.page.Page;
import edu.univ.erp.ui.page.Shell;
import net.miginfocom.swing.MigLayout;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The course catalogue. */
public class CoursesPage extends Page {

    private List<Course> courses = List.of();
    private Map<Integer, Integer> sectionCounts = new HashMap<>();
    private final DataTable<Course> table = new DataTable<>();
    private final JTextField search = UI.search("Search code, title or department");

    public CoursesPage(Shell shell) {
        super(shell);
        table.strong("Code", 100, Course::code)
                .col("Title", 300, Course::title)
                .num("Credits", 80, Course::credits)
                .badge("Department", 170, c -> c.department() == null ? "General" : c.department(),
                        v -> edu.univ.erp.ui.kit.Avatar.colorFor(String.valueOf(v)))
                .num("Sections", 90, c -> sectionCounts.getOrDefault(c.id(), 0))
                .col("Description", 360, Course::description)
                .search(c -> c.code() + " " + c.title() + " " + c.department())
                .empty("book", "No courses", "Add your first course to start scheduling sections.")
                .onDoubleClick(this::form)
                .menu(c -> List.of(DropdownMenu.Item.of("edit", "Edit course", () -> form(c)),
                        DropdownMenu.Item.separator(),
                        DropdownMenu.Item.danger("trash", "Delete course", () -> confirmDelete(c))));
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                table.filter(search.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                table.filter(search.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
    }

    @Override
    public String title() {
        return "Courses";
    }

    @Override
    public String subtitle() {
        return courses.size() + " courses in the catalogue";
    }

    @Override
    public void reload() {
        load(() -> {
            Map<Integer, Integer> counts = new HashMap<>();
            for (Section s : AdminService.sections()) counts.merge(s.courseId(), 1, Integer::sum);
            return new Object[]{AdminService.courses(), counts};
        }, r -> {
            @SuppressWarnings("unchecked")
            List<Course> c = (List<Course>) r[0];
            courses = c;
            @SuppressWarnings("unchecked")
            Map<Integer, Integer> m = (Map<Integer, Integer>) r[1];
            sectionCounts = m;
            shell.refreshTitle(this);
            JPanel col = column();
            JPanel bar = new JPanel(new MigLayout("insets 0, fillx", "[grow, fill]push[]", "[center]"));
            bar.setOpaque(false);
            bar.add(search, "h 40!, wmax 460");
            Btn add = Btn.primary("Add course", "plus");
            add.addActionListener(e -> form(null));
            bar.add(add);
            col.add(bar);
            Card card = new Card(new MigLayout("insets 0, fill", "[grow, fill]", "[grow, fill]"));
            card.pad(6, 6, 6, 6);
            card.add(table, "h 620!");
            col.add(card);
            table.setRows(courses);
            table.filter(search.getText());
            setBody(col, true);
        });
    }

    private void form(Course existing) {
        boolean create = existing == null;
        Modal m = new Modal(this, create ? "Add course" : "Edit " + existing.code(), null, create ? "plus" : "edit").width(600);
        JPanel f = new JPanel(new MigLayout("insets 0, fillx, wrap 3, gap 14", "[160!, fill][grow, fill][110!, fill]", ""));
        f.setOpaque(false);
        JTextField code = UI.field("e.g. CSE101", null);
        JTextField title = UI.field("Course title", null);
        JTextField credits = UI.field("4", null);
        JTextField dept = UI.field("e.g. Computer Science", null);
        JTextArea desc = UI.area(4);
        if (!create) {
            code.setText(existing.code());
            title.setText(existing.title());
            credits.setText(String.valueOf(existing.credits()));
            dept.setText(existing.department() == null ? "" : existing.department());
            desc.setText(existing.description() == null ? "" : existing.description());
        }
        f.add(UI.labeled("Code", code, null));
        f.add(UI.labeled("Title", title, null));
        f.add(UI.labeled("Credits", credits, null));
        f.add(UI.labeled("Department", dept, null), "span 3");
        f.add(UI.labeledArea("Description", UI.areaScroll(desc)), "span 3, h 130!");
        m.content(f);
        m.cancel("Cancel");
        Btn ok = Btn.primary(create ? "Add course" : "Save changes", "check");
        ok.addActionListener(e -> {
            int cr;
            try {
                cr = Integer.parseInt(credits.getText().trim());
            } catch (NumberFormatException ex) {
                Toasts.error(this, "Credits must be a whole number.");
                return;
            }
            Async.button(ok, () -> AdminService.saveCourse(create ? null : existing.id(), code.getText(), title.getText(), cr,
                    desc.getText(), dept.getText()), id -> {
                m.close();
                Toasts.success(this, create ? "Course added." : "Course updated.");
                reload();
            });
        });
        m.primary(ok);
        m.show();
    }

    private void confirmDelete(Course c) {
        Modal.confirm(this, "Delete " + c.code() + "?", UI.esc(c.title()) + " will be removed from the catalogue.",
                "Delete", true, () -> Async.run(this, () -> {
                    AdminService.deleteCourse(c.id());
                    return true;
                }, ok -> {
                    Toasts.success(this, "Deleted " + c.code() + ".");
                    reload();
                }));
    }
}
