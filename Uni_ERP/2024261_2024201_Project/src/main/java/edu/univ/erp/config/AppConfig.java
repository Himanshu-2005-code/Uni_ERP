package edu.univ.erp.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Database connection settings.
 *
 * Values are read, highest priority first, from: JVM system properties
 * (-Ddb.host=...), environment variables (ERP_DB_HOST...), ./erp.properties,
 * ~/.campus-erp/erp.properties, then built-in defaults. Nothing is hard-coded
 * in the source any more; the first-run setup screen writes the user file.
 */
public final class AppConfig {

    public static final String AUTH_SCHEMA = "university_auth";
    public static final String ERP_SCHEMA = "university_erp";

    private static final Path USER_DIR = Paths.get(System.getProperty("user.home"), ".campus-erp");
    private static final Path USER_FILE = USER_DIR.resolve("erp.properties");
    private static final Path LOCAL_FILE = Paths.get("erp.properties");

    private static AppConfig instance;

    private String host = "localhost";
    private int port = 3306;
    private String user = "root";
    private String password = "";
    private String urlParams = "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private AppConfig() {
    }

    public static synchronized AppConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static AppConfig load() {
        AppConfig c = new AppConfig();
        Properties p = new Properties();
        readInto(p, USER_FILE);
        readInto(p, LOCAL_FILE);
        c.host = pick(p, "db.host", "ERP_DB_HOST", c.host);
        c.port = Integer.parseInt(pick(p, "db.port", "ERP_DB_PORT", String.valueOf(c.port)));
        c.user = pick(p, "db.user", "ERP_DB_USER", c.user);
        c.password = pick(p, "db.password", "ERP_DB_PASSWORD", c.password);
        c.urlParams = pick(p, "db.params", "ERP_DB_PARAMS", c.urlParams);
        return c;
    }

    private static void readInto(Properties p, Path file) {
        if (Files.isRegularFile(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (IOException ignored) {
                // unreadable file: fall back to the other sources
            }
        }
    }

    private static String pick(Properties p, String key, String env, String def) {
        String v = System.getProperty(key);
        if (v == null) v = System.getenv(env);
        if (v == null) v = p.getProperty(key);
        return v == null ? def : v.trim();
    }

    /** Saves the connection settings to ~/.campus-erp/erp.properties. */
    public void save() throws IOException {
        Files.createDirectories(USER_DIR);
        Properties p = new Properties();
        p.setProperty("db.host", host);
        p.setProperty("db.port", String.valueOf(port));
        p.setProperty("db.user", user);
        p.setProperty("db.password", password);
        p.setProperty("db.params", urlParams);
        try (OutputStream out = Files.newOutputStream(USER_FILE)) {
            p.store(out, "Campus ERP database connection");
        }
    }

    public String serverUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/?" + urlParams;
    }

    public String erpUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + ERP_SCHEMA + "?" + urlParams;
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getUser() { return user; }
    public String getPassword() { return password; }
    public Path getUserFile() { return USER_FILE; }

    public void setHost(String host) { this.host = host; }
    public void setPort(int port) { this.port = port; }
    public void setUser(String user) { this.user = user; }
    public void setPassword(String password) { this.password = password; }
}
