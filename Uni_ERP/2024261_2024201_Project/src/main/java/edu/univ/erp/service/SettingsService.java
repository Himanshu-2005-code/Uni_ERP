package edu.univ.erp.service;

import edu.univ.erp.data.SettingsDAO;
import edu.univ.erp.db.Database;
import edu.univ.erp.domain.Role;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/** Institution-wide settings, cached and refreshed on demand. */
public final class SettingsService {

    public static final String MAINTENANCE = "maintenance_mode";
    public static final String INSTITUTION = "institution_name";
    public static final String SEMESTER = "current_semester";
    public static final String YEAR = "current_year";
    public static final String ADD_DEADLINE = "add_deadline";
    public static final String DROP_DEADLINE = "drop_deadline";
    public static final String MAX_CREDITS = "max_credits";
    public static final String LOCKOUT = "lockout_threshold";

    private static final SettingsService INSTANCE = new SettingsService();

    private volatile Map<String, String> values = new HashMap<>();

    private SettingsService() {
    }

    public static SettingsService get() {
        return INSTANCE;
    }

    public SettingsService refresh() {
        values = Database.read(SettingsDAO::all);
        return this;
    }

    private String value(String key, String def) {
        String v = values.get(key);
        return v == null || v.isBlank() ? def : v;
    }

    public boolean maintenance() {
        refresh();
        return Boolean.parseBoolean(value(MAINTENANCE, "false"));
    }

    public String institution() { return value(INSTITUTION, "Campus University"); }
    public String semester() { return value(SEMESTER, "Monsoon"); }
    public int year() { return intValue(YEAR, LocalDate.now().getYear()); }
    public String term() { return semester() + " " + year(); }
    public LocalDate addDeadline() { return date(ADD_DEADLINE); }
    public LocalDate dropDeadline() { return date(DROP_DEADLINE); }
    public int maxCredits() { return intValue(MAX_CREDITS, 24); }
    public int lockoutThreshold() { return intValue(LOCKOUT, 5); }
    public boolean demoData() { return Boolean.parseBoolean(value("demo_data", "false")); }

    public Map<String, String> snapshot() {
        return new HashMap<>(values);
    }

    private int intValue(String key, int def) {
        try {
            return Integer.parseInt(value(key, String.valueOf(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private LocalDate date(String key) {
        try {
            return LocalDate.parse(value(key, ""));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public void setMaintenance(boolean on) {
        Session.require(Role.ADMIN);
        Database.txRun(c -> {
            SettingsDAO.set(c, MAINTENANCE, Boolean.toString(on));
            Audit.log(c, "MAINTENANCE", on ? "Maintenance mode turned on" : "Maintenance mode turned off");
        });
        refresh();
    }

    /** Validates and saves academic settings. */
    public void update(String institution, String semester, int year, LocalDate addDeadline,
                       LocalDate dropDeadline, int maxCredits, int lockout) {
        Session.require(Role.ADMIN);
        if (institution == null || institution.isBlank()) throw new ServiceException("Institution name is required.");
        if (semester == null || semester.isBlank()) throw new ServiceException("Semester is required.");
        if (year < 2000 || year > 2100) throw new ServiceException("Year must be between 2000 and 2100.");
        if (maxCredits < 1 || maxCredits > 60) throw new ServiceException("Credit limit must be between 1 and 60.");
        if (lockout < 1 || lockout > 20) throw new ServiceException("Lockout threshold must be between 1 and 20.");
        if (addDeadline != null && dropDeadline != null && dropDeadline.isBefore(addDeadline)) {
            throw new ServiceException("The drop deadline can't be before the add deadline.");
        }
        Database.txRun(c -> {
            SettingsDAO.set(c, INSTITUTION, institution.trim());
            SettingsDAO.set(c, SEMESTER, semester.trim());
            SettingsDAO.set(c, YEAR, String.valueOf(year));
            SettingsDAO.set(c, ADD_DEADLINE, addDeadline == null ? "" : addDeadline.toString());
            SettingsDAO.set(c, DROP_DEADLINE, dropDeadline == null ? "" : dropDeadline.toString());
            SettingsDAO.set(c, MAX_CREDITS, String.valueOf(maxCredits));
            SettingsDAO.set(c, LOCKOUT, String.valueOf(lockout));
            Audit.log(c, "SETTINGS", "Term " + semester + " " + year + ", add by " + addDeadline + ", drop by "
                    + dropDeadline + ", max " + maxCredits + " credits");
        });
        refresh();
    }
}
