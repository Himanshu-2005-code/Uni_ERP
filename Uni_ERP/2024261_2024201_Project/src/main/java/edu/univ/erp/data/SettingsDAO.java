package edu.univ.erp.data;

import edu.univ.erp.db.Sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class SettingsDAO {

    private SettingsDAO() {
    }

    public static Map<String, String> all(Connection c) throws SQLException {
        Map<String, String> m = new HashMap<>();
        Sql.list(c, "SELECT setting_key, setting_value FROM settings",
                rs -> m.put(rs.getString(1), rs.getString(2)));
        return m;
    }

    public static void set(Connection c, String key, String value) throws SQLException {
        Sql.update(c, "INSERT INTO settings (setting_key, setting_value) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)", key, value);
    }
}
