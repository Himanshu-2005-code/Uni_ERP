package edu.univ.erp.ui.page.admin;

import com.formdev.flatlaf.FlatClientProperties;
import edu.univ.erp.domain.Course;
import edu.univ.erp.domain.InstructorProfile;
import edu.univ.erp.domain.Section;
import edu.univ.erp.service.AdminService;
import edu.univ.erp.service.SettingsService;
import edu.univ.erp.ui.kit.Async;
import edu.univ.erp.ui.kit.Btn;
import edu.univ.erp.ui.kit.Card;
import edu.univ.erp.ui.kit.DataTable;
import edu.univ.erp.ui.kit.DropdownMenu;
import edu.univ.erp.ui.kit.Modal;
import edu.univ.erp.ui.kit.Select;
import edu.univ.erp.ui.kit.Toasts;
import edu.univ.erp.ui.kit.UI;
import edu.univ.erp.ui.page.Page;
import edu.univ.erp.ui.page.Shell;
import edu.univ.erp.ui.theme.Theme;
import edu.univ.erp.util.TimeSlot;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Scheduling: every section of every course. */
public class SectionsAdminPage extends Page {

    private record Data(List<Section> sections, List<Course> courses, List<InstructorProfile> instructors) {
    }

    private static final String ALL = "All terms";
    private Data data;
    private final DataTable<Section> table = new DataTable<>();
    private final Select<String> term = new Select<String>().leading("calendar");
    private final JTextField search = UI.search("Search course, instructor or room");

    public SectionsAdminPage(Shell shell) {
        super(shell);
        table.strong("Code", 86, Section::courseCode)
                .col("Course", 210, Section::courseTitle)
                .person("Instructor", 200, s -> s.instructorName() == null ? "Unassigned" : s.instructorName(), Section::department)
                .col("Schedule", 190, Section::dayTime)
                .col("Room", 100, Section::room)
                .col("Term", 140, Section::term)
                .progress("Seats", 170, Section::fillRatio, s -> s.enrolledCount() + " / " + s.capacity())
                .search(s -> s.courseCode() + " " + s.courseTitle() + " " + s.instructorName() + " " + s.room())
                .empty("calendar", "No sections", "Add sections to put courses on the timetable.")
                .onDoubleClick(this::form)
                .menu(s -> List.of(DropdownMenu.Item.of("edit", "Edit section", () -> form(s)),
                        DropdownMenu.Item.of("plus", "Duplicate", () -> duplicate(s)),
                        DropdownMenu.Item.separator(),
                        DropdownMenu.Item.danger("trash", "Delete section", () -> confirmDelete(s))));
        term.onChange(t -> applyFilter());
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
    }

    @Override
    public String title() {
        return "Sections & timetable";
    }

    @Override
    public String subtitle() {
        return data == null ? "Who teaches what, where and when" : data.sections().size() + " sections across all terms";
    }

    @Override
    public void reload() {
        load(() -> new Data(AdminService.sections(), AdminService.courses(), AdminService.instructors()), d -> {
            data = d;
            shell.refreshTitle(this);
            List<String> terms = new ArrayList<>();
            terms.add(ALL);
            for (Section s : d.sections()) if (!terms.contains(s.term())) terms.add(s.term());
            String keep = term.selected();
            term.options(terms);
            String current = SettingsService.get().term();
            term.setSelectedQuietly(keep != null && terms.contains(keep) ? keep : terms.contains(current) ? current : ALL);
            JPanel col = column();
            JPanel bar = new JPanel(new MigLayout("insets 0, fillx", "[220!][grow, fill]push[]", "[center]"));
            bar.setOpaque(false);
            bar.add(term, "h 40!");
            bar.add(search, "h 40!, gapleft 12, wmax 420");
            Btn add = Btn.primary("Add section", "plus");
            add.addActionListener(e -> form(null));
            bar.add(add);
            col.add(bar);
            Card c = new Card(new MigLayout("insets 0, fill", "[grow, fill]", "[grow, fill]"));
            c.pad(6, 6, 6, 6);
            c.add(table, "h 620!");
            col.add(c);
            table.setRows(d.sections());
            applyFilter();
            setBody(col, true);
        });
    }

    private void applyFilter() {
        String t = term.selected();
        String q = search.getText().trim().toLowerCase();
        table.filter(s -> (t == null || t.equals(ALL) || s.term().equals(t))
                && (q.isEmpty() || (s.courseCode() + " " + s.courseTitle() + " " + s.instructorName() + " " + s.room())
                .toLowerCase().contains(q)));
    }

    private void duplicate(Section s) {
        form(s, true);
    }

    private void form(Section existing) {
        form(existing, false);
    }

    private void form(Section existing, boolean copy) {
        boolean create = existing == null || copy;
        Modal m = new Modal(this, create ? (copy ? "Duplicate section" : "Add section") : "Edit section #" + existing.id(),
                "Room and instructor clashes are checked automatically.", create ? "plus" : "edit").width(660);
        JPanel f = new JPanel(new MigLayout("insets 0, fillx, wrap 2, gap 14", "[grow, fill, sg a][grow, fill, sg a]", ""));
        f.setOpaque(false);
        Select<Course> course = new Select<Course>().labels(c -> c.code() + " · " + c.title()).options(data.courses())
                .placeholder("Choose a course");
        List<InstructorProfile> people = new ArrayList<>();
        people.add(null);
        people.addAll(data.instructors());
        Select<InstructorProfile> inst = new Select<InstructorProfile>()
                .labels(i -> i == null ? "Unassigned" : i.displayName() + (i.department() == null ? "" : " · " + i.department()))
                .options(people).placeholder("Unassigned");
        List<JToggleButton> days = new ArrayList<>();
        JPanel dayRow = UI.clear(new MigLayout("insets 0, gap 6", "", ""));
        for (DayOfWeek d : DayOfWeek.values()) {
            if (d == DayOfWeek.SUNDAY) continue;
            JToggleButton b = new JToggleButton(d.name().charAt(0) + d.name().substring(1, 3).toLowerCase());
            b.putClientProperty(FlatClientProperties.STYLE, "arc: 999; margin: 6,12,6,12; borderWidth: 1; focusWidth: 0");
            b.setFont(Theme.semibold(12f));
            days.add(b);
            dayRow.add(b, "h 36!");
        }
        List<String> times = new ArrayList<>();
        for (int t = 7 * 60; t <= 21 * 60; t += 30) times.add(TimeSlot.format(t));
        Select<String> start = new Select<String>().options(times).leading("clock");
        Select<String> end = new Select<String>().options(times).leading("clock");
        JTextField room = UI.field("e.g. LHC-101", "pin");
        JTextField cap = UI.field("60", "users");
        Select<String> sem = new Select<String>().options(List.of("Monsoon", "Winter", "Summer"));
        JTextField year = UI.field(String.valueOf(SettingsService.get().year()), null);
        JLabel preview = UI.muted(" ");
        Runnable updatePreview = () -> preview.setText(slot(days, start, end) == null ? "Pick at least one day and an end time after the start."
                : "Meets " + slot(days, start, end));
        for (JToggleButton b : days) b.addActionListener(e -> updatePreview.run());
        start.onChange(v -> updatePreview.run());
        end.onChange(v -> updatePreview.run());

        if (existing != null) {
            course.setSelectedQuietly(data.courses().stream().filter(c -> c.id() == existing.courseId()).findFirst().orElse(null));
            inst.setSelectedQuietly(data.instructors().stream().filter(i -> Objects.equals(i.userId(), existing.instructorId()))
                    .findFirst().orElse(null));
            TimeSlot t = TimeSlot.parse(existing.dayTime());
            if (t != null) {
                for (DayOfWeek d : t.days()) if (d.getValue() <= 6) days.get(d.getValue() - 1).setSelected(true);
                start.setSelectedQuietly(TimeSlot.format(t.startMinute()));
                end.setSelectedQuietly(TimeSlot.format(t.endMinute()));
            }
            room.setText(copy ? "" : existing.room());
            cap.setText(String.valueOf(existing.capacity()));
            sem.setSelectedQuietly(existing.semester());
            year.setText(String.valueOf(existing.year()));
        } else {
            start.setSelectedQuietly("10:00");
            end.setSelectedQuietly("11:30");
            sem.setSelectedQuietly(SettingsService.get().semester());
        }
        updatePreview.run();
        f.add(UI.labeled("Course", course, null), "span 2");
        f.add(UI.labeled("Instructor", inst, null), "span 2");
        JPanel sched = UI.clear(new MigLayout("insets 0, fillx, wrap 1, gap 6", "[grow, fill]", ""));
        JLabel sl = new JLabel("Days");
        sl.setFont(Theme.medium(12.5f));
        sched.add(sl);
        sched.add(dayRow);
        f.add(sched, "span 2");
        f.add(UI.labeled("Starts", start, null));
        f.add(UI.labeled("Ends", end, null));
        f.add(preview, "span 2");
        f.add(UI.labeled("Room", room, null));
        f.add(UI.labeled("Capacity", cap, null));
        f.add(UI.labeled("Semester", sem, null));
        f.add(UI.labeled("Year", year, null));
        m.content(f);
        m.cancel("Cancel");
        Btn ok = Btn.primary(create ? "Create section" : "Save changes", "check");
        ok.addActionListener(e -> {
            if (course.selected() == null) {
                Toasts.error(this, "Choose a course.");
                return;
            }
            String s = slot(days, start, end);
            if (s == null) {
                Toasts.error(this, "Pick the days and a valid time range.");
                return;
            }
            int capacity;
            int y;
            try {
                capacity = Integer.parseInt(cap.getText().trim());
                y = Integer.parseInt(year.getText().trim());
            } catch (NumberFormatException ex) {
                Toasts.error(this, "Capacity and year must be numbers.");
                return;
            }
            AdminService.SectionForm form = new AdminService.SectionForm(course.selected().id(),
                    inst.selected() == null ? null : inst.selected().userId(), s, room.getText(), capacity, sem.selected(), y);
            Async.button(ok, () -> AdminService.saveSection(create ? null : existing.id(), form), id -> {
                m.close();
                Toasts.success(this, create ? "Section created." : "Section updated.");
                reload();
            });
        });
        m.primary(ok);
        m.show();
    }

    private static String slot(List<JToggleButton> days, Select<String> start, Select<String> end) {
        StringBuilder sb = new StringBuilder();
        for (JToggleButton b : days) if (b.isSelected()) sb.append(b.getText()).append(' ');
        if (sb.length() == 0 || start.selected() == null || end.selected() == null) return null;
        String s = sb + start.selected() + "-" + end.selected();
        return TimeSlot.isValid(s) ? TimeSlot.parse(s).toString() : null;
    }

    private void confirmDelete(Section s) {
        Modal.confirm(this, "Delete section #" + s.id() + "?", s.courseCode() + " · " + s.dayTime() + " · " + s.room()
                + "<br>Only sections with no registration records can be deleted.", "Delete", true,
                () -> Async.run(this, () -> {
                    AdminService.deleteSection(s.id());
                    return true;
                }, ok -> {
                    Toasts.success(this, "Section deleted.");
                    reload();
                }));
    }
}
