package edu.univ.erp.ui.screen;

import edu.univ.erp.config.AppConfig;
import edu.univ.erp.db.Database;
import edu.univ.erp.ui.kit.Btn;
import edu.univ.erp.ui.theme.Theme;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.sql.Connection;

/** Shown when the database can't be reached: edit and test the connection settings. */
class ConnectScreen extends SceneScreen {

    ConnectScreen(AppFrame frame, String error) {
        super(500);
        AppConfig cfg = AppConfig.get();
        card.add(Glass.label("Database connection", 24f, true, Glass.TEXT));
        card.add(Glass.label("<html><div style='width:308px'>Campus ERP stores everything in MySQL (or MariaDB). "
                + "Enter the server details; the databases are created automatically.</div></html>", 13f, false,
                Glass.MUTED), "gapbottom 18");
        JLabel err = Glass.label(" ", 12.5f, false, Theme.hex("#FCA5A5"));
        if (error != null) err.setText("<html><div style='width:308px'>Couldn't connect: " + error + "</div></html>");
        JTextField host = Glass.field("localhost", "server");
        host.setText(cfg.getHost());
        JTextField port = Glass.field("3306", "dot");
        port.setText(String.valueOf(cfg.getPort()));
        JTextField user = Glass.field("root", "user");
        user.setText(cfg.getUser());
        JPasswordField pw = Glass.password("Password");
        pw.setText(cfg.getPassword());
        JPanel row = new JPanel(new MigLayout("insets 0, fillx, gap 12", "[grow, fill][110!]", ""));
        row.setOpaque(false);
        row.add(field("Host", host));
        row.add(field("Port", port));
        card.add(row, "gapbottom 12");
        card.add(field("Username", user), "gapbottom 12");
        card.add(field("Password", pw), "gapbottom 8");
        card.add(err, "gapbottom 10");
        Btn go = Btn.primary("Test & connect", "zap");
        card.add(go, "h 48!");
        JLabel hint = Glass.label("Saved to " + cfg.getUserFile(), 11f, false, Theme.alpha(Glass.MUTED, 0.8));
        card.add(hint, "gaptop 14, center");
        go.addActionListener(e -> {
            int p;
            try {
                p = Integer.parseInt(port.getText().trim());
            } catch (NumberFormatException ex) {
                err.setText("Port must be a number.");
                return;
            }
            cfg.setHost(host.getText().trim().isEmpty() ? "localhost" : host.getText().trim());
            cfg.setPort(p);
            cfg.setUser(user.getText().trim());
            cfg.setPassword(new String(pw.getPassword()));
            go.setLoading(true);
            err.setText(" ");
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    Database.stop();
                    try (Connection c = Database.serverConnection()) {
                        c.isValid(3);
                    }
                    cfg.save();
                    return null;
                }

                @Override
                protected void done() {
                    go.setLoading(false);
                    try {
                        get();
                        frame.start();
                    } catch (Exception ex) {
                        Throwable c = ex.getCause() == null ? ex : ex.getCause();
                        err.setText("<html><div style='width:308px'>" + c.getMessage() + "</div></html>");
                    }
                }
            }.execute();
        });
    }

    private static JPanel field(String label, java.awt.Component f) {
        JPanel p = new JPanel(new MigLayout("insets 0, fillx, wrap 1, gap 6", "[grow, fill]", ""));
        p.setOpaque(false);
        p.add(Glass.label(label, 12.5f, true, Glass.MUTED));
        p.add(f, "h 46!");
        return p;
    }
}
