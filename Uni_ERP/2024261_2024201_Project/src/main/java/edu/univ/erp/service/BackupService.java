package edu.univ.erp.service;

import edu.univ.erp.config.AppConfig;
import edu.univ.erp.data.AuditDAO;
import edu.univ.erp.db.Database;
import edu.univ.erp.domain.Role;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Full logical backup of both schemas to a .sql file, written in pure Java so it works
 * without mysqldump installed. The file can also be loaded with the mysql command-line client.
 */
public final class BackupService {

    public record Summary(Path file, int tables, long rows, long bytes) {
    }

    private static final String[] SCHEMAS = {AppConfig.AUTH_SCHEMA, AppConfig.ERP_SCHEMA};

    private BackupService() {
    }

    public static Path defaultFolder() {
        return Paths.get(System.getProperty("user.home"), ".campus-erp", "backups");
    }

    public static String suggestedName() {
        return "campus-erp-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".sql";
    }

    public static Summary backup(Path file) {
        Session.require(Role.ADMIN);
        Summary s = dump(file);
        Database.txRun(c -> Audit.log(c, "BACKUP", file.getFileName() + " (" + s.rows() + " rows)"));
        return s;
    }

    private static Summary dump(Path file) {
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            int tables = 0;
            long rows = 0;
            try (Connection c = Database.connection();
                 Writer w = new BufferedWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
                w.write("-- Campus ERP backup, " + LocalDateTime.now() + "\n");
                w.write("SET FOREIGN_KEY_CHECKS=0;\n");
                // Drop in reverse so dependent academic tables go before the accounts table
                for (int i = SCHEMAS.length - 1; i >= 0; i--) {
                    for (String t : tables(c, SCHEMAS[i])) {
                        w.write("DROP TABLE IF EXISTS `" + SCHEMAS[i] + "`.`" + t + "`;\n");
                    }
                }
                for (String schema : SCHEMAS) {
                    w.write("CREATE DATABASE IF NOT EXISTS `" + schema + "`;\nUSE `" + schema + "`;\n");
                    for (String t : tables(c, schema)) {
                        tables++;
                        try (Statement s = c.createStatement();
                             ResultSet rs = s.executeQuery("SHOW CREATE TABLE `" + schema + "`.`" + t + "`")) {
                            rs.next();
                            w.write(rs.getString(2) + ";\n");
                        }
                        rows += dumpRows(c, schema, t, w);
                    }
                }
                w.write("SET FOREIGN_KEY_CHECKS=1;\n");
            }
            return new Summary(file, tables, rows, Files.size(file));
        } catch (SQLException | IOException e) {
            throw new ServiceException("Backup failed: " + e.getMessage(), e);
        }
    }

    private static List<String> tables(Connection c, String schema) throws SQLException {
        List<String> out = new ArrayList<>();
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SHOW FULL TABLES FROM `" + schema + "` WHERE Table_type = 'BASE TABLE'")) {
            while (rs.next()) out.add(rs.getString(1));
        }
        return out;
    }

    private static long dumpRows(Connection c, String schema, String table, Writer w) throws SQLException, IOException {
        long n = 0;
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM `" + schema + "`.`" + table + "`")) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            StringBuilder head = new StringBuilder("INSERT INTO `" + table + "` (");
            for (int i = 1; i <= cols; i++) head.append(i > 1 ? ", " : "").append('`').append(md.getColumnName(i)).append('`');
            head.append(") VALUES ");
            StringBuilder batch = new StringBuilder();
            int inBatch = 0;
            while (rs.next()) {
                batch.append(inBatch > 0 ? ", " : "").append('(');
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) batch.append(", ");
                    batch.append(literal(rs.getObject(i)));
                }
                batch.append(')');
                n++;
                if (++inBatch == 200) {
                    w.write(head + batch.toString() + ";\n");
                    batch.setLength(0);
                    inBatch = 0;
                }
            }
            if (inBatch > 0) w.write(head + batch.toString() + ";\n");
        }
        return n;
    }

    static String literal(Object v) {
        if (v == null) return "NULL";
        if (v instanceof Number) return v.toString();
        if (v instanceof Boolean b) return b ? "1" : "0";
        if (v instanceof byte[] bytes) {
            StringBuilder sb = new StringBuilder("0x");
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return bytes.length == 0 ? "''" : sb.toString();
        }
        String s = v instanceof LocalDateTime t ? t.toString().replace('T', ' ') : v.toString();
        StringBuilder sb = new StringBuilder("'");
        for (char ch : s.toCharArray()) {
            switch (ch) {
                case '\'' -> sb.append("\\'");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\0' -> sb.append("\\0");
                case '\u001a' -> sb.append("\\Z");
                default -> sb.append(ch);
            }
        }
        return sb.append('\'').toString();
    }

    /**
     * Restores a backup. A safety copy of the current data is written first, because
     * MySQL can't roll back table drops. Returns the path of that safety copy.
     */
    public static Path restore(Path file) {
        Session.require(Role.ADMIN);
        String sql;
        try {
            sql = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ServiceException("Couldn't read " + file.getFileName() + ": " + e.getMessage(), e);
        }
        if (!sql.startsWith("-- Campus ERP backup")) {
            throw new ServiceException("That file isn't a Campus ERP backup.");
        }
        Path safety = defaultFolder().resolve("before-restore-" + suggestedName());
        dump(safety);
        String who = Session.user().username();
        int uid = Session.user().id();
        try (Connection c = Database.connection(); Statement s = c.createStatement()) {
            for (String stmt : split(sql)) {
                s.execute(stmt);
            }
            s.execute("USE `" + AppConfig.ERP_SCHEMA + "`");
            AuditDAO.log(c, uid, who, "RESTORE", file.getFileName() + " (safety copy " + safety.getFileName() + ")");
        } catch (SQLException e) {
            throw new ServiceException("Restore failed: " + e.getMessage() + ". Your previous data was saved to "
                    + safety, e);
        }
        Database.stop(); // pooled connections may still point at dropped tables
        Database.start();
        return safety;
    }

    /** Splits a script on semicolons that are outside quotes and comments. */
    static List<String> split(String sql) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (quote != 0) {
                cur.append(ch);
                if (ch == '\\' && i + 1 < sql.length()) {
                    cur.append(sql.charAt(++i));
                } else if (ch == quote) {
                    quote = 0;
                }
                continue;
            }
            if (ch == '-' && sql.startsWith("--", i)) {
                int nl = sql.indexOf('\n', i);
                i = nl < 0 ? sql.length() : nl;
                continue;
            }
            if (ch == '\'' || ch == '"' || ch == '`') {
                quote = ch;
                cur.append(ch);
            } else if (ch == ';') {
                if (!cur.toString().isBlank()) out.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        if (!cur.toString().isBlank()) out.add(cur.toString().trim());
        return out;
    }
}
