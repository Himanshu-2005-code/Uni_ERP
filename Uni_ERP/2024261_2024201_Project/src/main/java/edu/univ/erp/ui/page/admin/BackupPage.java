package edu.univ.erp.ui.page.admin;

import edu.univ.erp.service.BackupService;
import edu.univ.erp.ui.kit.Async;
import edu.univ.erp.ui.kit.Btn;
import edu.univ.erp.ui.kit.DataTable;
import edu.univ.erp.ui.kit.DropdownMenu;
import edu.univ.erp.ui.kit.Modal;
import edu.univ.erp.ui.kit.Toasts;
import edu.univ.erp.ui.kit.UI;
import edu.univ.erp.ui.page.Page;
import edu.univ.erp.ui.page.Parts;
import edu.univ.erp.ui.page.Shell;
import edu.univ.erp.ui.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Full database backups to .sql files, and restores with a safety copy. */
public class BackupPage extends Page {

    private record BackupFile(Path path, long size, LocalDateTime modified) {
    }

    public BackupPage(Shell shell) {
        super(shell);
    }

    @Override
    public String title() {
        return "Backup & restore";
    }

    @Override
    public String subtitle() {
        return "Everything, in one portable SQL file";
    }

    @Override
    public void reload() {
        load(this::backups, this::render);
    }

    private List<BackupFile> backups() {
        Path dir = BackupService.defaultFolder();
        List<BackupFile> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) return out;
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> p.toString().endsWith(".sql")).forEach(p -> {
                try {
                    out.add(new BackupFile(p, Files.size(p), LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(Files.getLastModifiedTime(p).toMillis()), ZoneId.systemDefault())));
                } catch (IOException ignored) {
                    // skip unreadable files
                }
            });
        } catch (IOException ignored) {
            // empty listing
        }
        out.sort(Comparator.comparing(BackupFile::modified).reversed());
        return out;
    }

    private void render(List<BackupFile> files) {
        JPanel col = column();
        JPanel row = new JPanel(new MigLayout("insets 0, fillx, gap 18", "[grow, fill, sg a][grow, fill, sg a]", "[top]"));
        row.setOpaque(false);
        Btn now = Btn.primary("Back up now", "download");
        now.addActionListener(e -> backup(now, BackupService.defaultFolder().resolve(BackupService.suggestedName())));
        Btn as = Btn.secondary("Save as…", "file");
        as.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(BackupService.defaultFolder().toFile());
            fc.setSelectedFile(new File(BackupService.suggestedName()));
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) backup(as, fc.getSelectedFile().toPath());
        });
        JPanel b = UI.clear(new MigLayout("insets 0, fillx, wrap 1, gap 12", "[grow, fill]", ""));
        b.add(UI.html("<div style='width:323px;line-height:1.45'>Writes both databases — accounts, courses, sections, "
                + "registrations, marks, notices and the audit log — to a single <b>.sql</b> file. It can be restored here "
                + "or loaded with the <code>mysql</code> command-line client.</div>", 13f, true));
        JPanel btns = UI.clear(new MigLayout("insets 0, gap 10", "", ""));
        btns.add(now);
        btns.add(as);
        b.add(btns);
        b.add(UI.muted("Default folder: " + BackupService.defaultFolder()));
        row.add(Parts.section("Create a backup", null, b));

        Btn from = Btn.danger("Restore from file…", "upload");
        from.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(BackupService.defaultFolder().toFile());
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) confirmRestore(fc.getSelectedFile().toPath());
        });
        JPanel r = UI.clear(new MigLayout("insets 0, fillx, wrap 1, gap 12", "[grow, fill]", ""));
        r.add(Parts.banner("alert", Theme.p().warning(), "Restoring <b>replaces all current data</b>. A safety backup of "
                + "the current state is taken automatically first, and you'll be signed out afterwards."));
        r.add(from, "w pref!");
        row.add(Parts.section("Restore", null, r));
        col.add(row);

        DataTable<BackupFile> t = new DataTable<BackupFile>()
                .person("File", 380, f -> f.path().getFileName().toString(), f -> f.path().getParent().toString())
                .col("Created", 200, f -> Parts.dateTime(f.modified()))
                .num("Size", 120, f -> size(f.size()))
                .badge("Kind", 160, f -> f.path().getFileName().toString().startsWith("before-restore") ? "Safety copy" : "Backup",
                        v -> "Backup".equals(v) ? Theme.series(0) : Theme.p().warning())
                .empty("database", "No backups yet", "Your backups will be listed here.")
                .onDoubleClick(f -> confirmRestore(f.path()))
                .menu(f -> List.of(DropdownMenu.Item.danger("upload", "Restore this backup", () -> confirmRestore(f.path()))));
        t.setRows(files);
        JPanel wrap = UI.clear(new MigLayout("insets 0, fill", "[grow, fill]", "[grow, fill]"));
        wrap.add(t, "h " + Math.max(220, Math.min(480, 60 + files.size() * 42)) + "!");
        col.add(Parts.section("Backups", files.size() + " in the default folder · right-click to restore", wrap));
        setBody(col, true);
    }

    private void backup(Btn b, Path file) {
        Async.button(b, () -> BackupService.backup(file), s -> {
            Toasts.success(this, "Backed up " + s.tables() + " tables and " + s.rows() + " rows (" + size(s.bytes()) + ").");
            reload();
        });
    }

    private void confirmRestore(Path file) {
        Modal m = new Modal(this, "Restore " + file.getFileName() + "?", "Type RESTORE to confirm. Current data will be "
                + "replaced (a safety copy is saved first).", "alert").width(500);
        JTextField confirm = UI.field("RESTORE", null);
        m.content(confirm);
        m.cancel("Cancel");
        Btn go = Btn.danger("Restore", "upload");
        go.setEnabled(false);
        confirm.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                go.setEnabled(confirm.getText().trim().equals("RESTORE"));
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                go.setEnabled(confirm.getText().trim().equals("RESTORE"));
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        go.addActionListener(e -> Async.button(go, () -> BackupService.restore(file), safety -> {
            m.close();
            Toasts.success(this, "Restored. Safety copy saved as " + safety.getFileName() + ". Please sign in again.");
            shell.frame().signOut();
        }));
        m.primary(go);
        m.show();
    }

    private static String size(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return String.format("%.1f KB", b / 1024.0);
        return String.format("%.1f MB", b / 1024.0 / 1024.0);
    }
}
